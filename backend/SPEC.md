\= SPEC-1: FCFS Sharing Platform
\:sectnums:
\:toc:

\== Background

A lightweight “first‑come‑first‑serve” (FCFS) sharing platform where individuals post items or accounts (e.g., “넷플릭스 계정 공유 4명 모음”, “안경 나눔 선착순 1개”) that others can claim.

Key ideas:

* Posts may be published immediately or scheduled for a future date/time.
* The first user to click **Claim** after the item opens wins; winner receives an instant e‑mail notification.
* Expected Day‑1 load: ≈1 000 concurrent online users.
* Fixed tech stack: **Spring Boot 3 + PostgreSQL + Redis + React JS**.
* Admins can moderate (delete or hide) inappropriate posts.
* Each post supports up to three images, free‑text description, and tag chips (e.g., 무료나눔).
* Front‑end displays canonical *server* time to avoid clock‑skew confusion.
* OAuth 2.0 social login will be used instead of classic e‑mail/password.
* Subscription model:

  * **Free** users → max **3 active posts** at any moment, **15 total posts** ever (rolling count).
  * **Premium** users → max **10 active posts** at any moment, **100 total posts** ever (rolling count).
* Once an item’s participant quota is filled, the system must hard‑lock further claims.

\== Requirements

\[EXISTING REQUIREMENTS SECTION KEPT UNCHANGED]

\== Method

\=== 1. High‑Level Architecture

## \[plantuml]

scale 80 width
!theme plain
actor User
actor Admin
rectangle "React SPA (Browser)" as UI
rectangle "Spring Boot API" as API
cloud "CDN / Object Storage" as CDN
collections "PostgreSQL" as PG
queue "Redis" as REDIS
rectangle "SMTP Server" as SMTP
User --> UI : Browse / Claim
Admin --> UI : Moderate
UI --> API : HTTPS (REST/JSON)
API --> REDIS : Lua Claim Script
API --> PG : JPA / SQL
API --> CDN : Pre‑signed Upload URLs
API --> SMTP : Sender (JavaMail)
--------------------------------

\=== 2. Data Model (simplified)

## \[source,sql]

CREATE TABLE user\_account (
id              BIGSERIAL PRIMARY KEY,
email           TEXT UNIQUE NOT NULL,
oauth\_provider  TEXT NOT NULL, -- kakao, google, apple
role            TEXT NOT NULL DEFAULT 'USER',
is\_premium      BOOLEAN NOT NULL DEFAULT FALSE,
created\_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE post (
id              BIGSERIAL PRIMARY KEY,
owner\_id        BIGINT REFERENCES user\_account(id),
title           TEXT NOT NULL,
description     TEXT NOT NULL,
quota           SMALLINT NOT NULL CHECK (quota > 0),
state           TEXT NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED | OPEN | CLOSED
open\_at         TIMESTAMPTZ NOT NULL,
close\_at        TIMESTAMPTZ,
tags            TEXT\[],
images          TEXT\[], -- up to 3 S3 keys
created\_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE claim (
id              BIGSERIAL PRIMARY KEY,
post\_id         BIGINT REFERENCES post(id) ON DELETE CASCADE,
user\_id         BIGINT REFERENCES user\_account(id) ON DELETE CASCADE,
claimed\_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
CONSTRAINT unique\_post\_user UNIQUE(post\_id,user\_id)
);
--

*`Redis key design`*::

\[cols="1,3"]
|===
|Key Pattern|Purpose
|`post:{id}:quota`|Integer counter initialised to `quota`; decremented atomically via Lua on each /claim
|`user:{id}:active_count`|Active posts per user (to enforce 3/10 limit)
|`post:{id}:state`|OPEN/CLOSED mirror for quick lookup by API Gateway
|===

*Atomic claim Lua* (pseudo):
\[source]
---------

if redis.call('GET', 'post:'..pid..'\:state') \~= 'OPEN' then return 0 end
if redis.call('DECR', 'post:'..pid..'\:quota') < 0 then return -1 end
\-- record winner ID in list for background sync
redis.call('RPUSH', 'post:'..pid..'\:winners', uid)
return 1
--------

A successful script returns `1`; `0` means not open yet; `-1` means quota exhausted concurrently (caller shows "closed")

\=== 3. Key Interactions

.**Schedule & Open**: `@Scheduled` fixedDelay (1 s) task scans `post.open_at`, transitions records to `OPEN`, seeds Redis keys.
.**Claim**: UI hits `POST /posts/{id}/claim`.  API executes Lua script.  If `1`, persist claim asynchronously.
.**Close**: When quota reaches 0, API updates `post.state=CLOSED`, persists winners, triggers email job.

\=== 4. Performance & Scaling

* MVP deployment: **Docker Compose** stack (`api`, `postgres`, `redis`, `nginx`, `smtp`) on a single host.
* Stateless Spring Boot container(s) behind Nginx for TLS termination.
* Redis single primary (local Docker service), Postgres 1 × primary.
* Horizontal scale (future): add more `api` containers; lift‑and‑shift to Kubernetes if traffic exceeds one host.
* Target p99 ≤100 ms claim latency attained by running Lua script inside Redis, avoiding PG round‑trip in critical path.

\== Implementation

\=== Repository layout

## \[source]

root/
backend/             # Spring Boot 3 service
src/main/java/…
src/main/resources/
build.gradle.kts
frontend/            # React (Vite + TypeScript)
src/
vite.config.ts
package.json
infra/
docker-compose.yml # Runtime stack
flyway/
V1\_\_init.sql
V2\_\_add\_claim\_limits.sql
----------------------------

\=== docker‑compose skeleton

## \[source,yaml]

version: '3.9'
services:
api:
build: ./backend
ports: \["8080:8080"]
env\_file: .env
depends\_on: \[db, redis]
frontend:
build: ./frontend
ports: \["3000:80"]
depends\_on: \[api]
db:
image: postgres:16
environment:
POSTGRES\_USER: fcfs
POSTGRES\_PASSWORD: secret
volumes:
\- pgdata:/var/lib/postgresql/data
redis:
image: redis:7
smtp:
image: namshi/smtp
volumes:
pgdata:
-------

\=== CI/CD pipeline (GitHub Actions)

1. **Backend**: Gradle build → unit tests → Docker image push.
2. **Frontend**: `npm ci && npm run build` → Docker image push.
3. **Infra**: Flyway migrations run on `db` service startup.
4. **Deploy**: `docker-compose pull && docker-compose up -d` on VPS.

\=== Local development

* `docker-compose up` spins full stack.
* Hot‑reload: Spring DevTools + Vite HMR.
* Seed data via `/infra/flyway` migrations + test fixtures.

\=== Observability

* **Logging**: JSON logs shipped to Loki.
* **Metrics**: Micrometer → Prometheus; Grafana dashboard for p99 latency.
* **Alerting**: Grafana alerts on 5xx rate >1% or p99 >200 ms.

\== Milestones

|===
|Week | Deliverable
|1 | Repo scaffolding, CI pipeline, basic React landing page
|2 | User OAuth2 login (Kakao, Google, Apple) and JWT session flow
|3 | Post creation UI/API, image upload, tag autocomplete
|4 | FCFS claim endpoint with Redis Lua lock, e‑mail notifications via SMTP
|5 | Admin moderation dashboard, rate limiting, basic observability
|6 | Load test to 1 000 concurrent users, performance tuning
|7 | Beta launch, bug‑fix sprint
|===

\== Gathering Results

* **Load‑test metrics**: p99 latency ≤100 ms, error rate <0.1% under 1 000 RPS peak.
* **User feedback**: collect via PostHog event funnels and in‑app survey.
* **Success criteria**: >500 successful claims in first month, zero duplicate‑winner incidents, <1‑hour mean time to recover (MTTR) for any outage.
