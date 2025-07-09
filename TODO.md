# FCFS Sharing Platform - TODO List

This document outlines the tasks required to build and deploy the FCFS sharing platform.

## Week 1: Scaffolding and Basic Setup

- [x] **Backend**: Initialize Spring Boot project with necessary dependencies (Spring Web, JPA, PostgreSQL, Redis, etc.).
- [x] **Backend**: Set up `build.gradle.kts`.
- [ ] **Frontend**: Initialize React project using Vite.
- [ ] **Frontend**: Create a basic landing page.
- [ ] **CI/CD**: Set up a basic GitHub Actions workflow to build and test both backend and frontend.
- [ ] **Infra**: Create the initial `docker-compose.yml` file.

## Week 2: User Authentication

- [x] **Backend**: Implement OAuth 2.0 login for Kakao, Google, and Naver.
- [x] **Backend**: Implement JWT generation and validation for session management.
- [ ] **Frontend**: Create login page and handle OAuth 2.0 redirects.
- [ ] **Frontend**: Store JWT and manage authenticated state.

## Week 3: Post Management

- [ ] **Backend**: Create `Post` entity and repository.
- [ ] **Backend**: Implement API endpoints for creating, reading, updating, and deleting posts.
- [ ] **Backend**: Implement image upload functionality (e.g., to S3-compatible storage).
- [ ] **Frontend**: Create a form for creating and editing posts.
- [ ] **Frontend**: Implement image uploads from the client.
- [ ] **Frontend**: Implement tag autocomplete feature.

## Week 4: Core FCFS Logic

- [ ] **Backend**: Implement the Redis Lua script for atomic claims.
- [ ] **Backend**: Create the `/posts/{id}/claim` endpoint.
- [ ] **Backend**: Implement the asynchronous job to persist claims and update post state.
- [ ] **Backend**: Set up the Java Mail Sender and implement email notifications for winners.
- [ ] **Frontend**: Add a "Claim" button to the post view.
- [ ] **Frontend**: Display real-time server time.

## Week 5: Administration and Observability

- [ ] **Backend**: Implement an admin role and secure admin endpoints.
- [ ] **Backend**: Create API endpoints for admins to moderate posts.
- [ ] **Backend**: Implement rate limiting on critical endpoints.
- [ ] **Frontend**: Create an admin dashboard to view and manage posts.
- [ ] **Observability**: Configure Micrometer and Prometheus for metrics.
- [ ] **Observability**: Set up Grafana for dashboards and alerting.

## Week 6: Testing and Performance

- [ ] **Testing**: Write unit and integration tests for the backend.
- [ ] **Testing**: Write component and end-to-end tests for the frontend.
- [ ] **Performance**: Conduct load testing to simulate 1,000 concurrent users.
- [ ] **Performance**: Analyze and optimize database queries and API response times.

## Week 7: Deployment and Launch

- [ ] **Infra**: Finalize `docker-compose.yml` for production.
- [ ] **Infra**: Set up Flyway for database migrations.
- [ ] **CI/CD**: Enhance GitHub Actions to build and push Docker images.
- [ ] **CI/CD**: Add a deployment step to the pipeline (`docker-compose pull && docker-compose up -d`).
- [ ] **Deployment**: Deploy the application to a VPS.
- [ ] **Launch**: Monitor the application and address any bugs.
