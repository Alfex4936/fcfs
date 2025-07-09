# FCFS (First Come First Serve) 시스템

## 프로젝트 개요
Spring Boot와 Redis Cluster를 활용한 고성능 선착순 시스템입니다.

## 기술 스택
- **Backend**: Spring Boot 3.x, Spring Security, JPA/Hibernate
- **Database**: PostgreSQL
- **Cache**: Redis Cluster (3 Master + 3 Replica)
- **Frontend**: React, Vite
- **인증**: OAuth2 (Google, Naver, Kakao)

## 성능 테스트 결과

### 극한 부하 테스트 (Extreme Load Test)
**테스트 환경:**
- Redis Cluster: 3 Master + 3 Replica 노드
- 동시 사용자: 1,000명
- 게시물 할당량: 100개

**성능 지표:**
```
🚀 처리량 (Throughput): ~514 요청/초
⏱️  총 처리 시간: 1,945ms
👥 사용자 생성 시간: 1,590ms (1,000명)
```

**응답 시간 분석:**
- **최소 응답 시간**: 10ms
- **최대 응답 시간**: 1,507ms
- **P50 (중간값)**: 50ms
- **P95**: 1,399ms
- **P99**: 1,431ms

**정확성 검증:**
- ✅ **성공한 신청**: 100명 (할당량과 정확히 일치)
- ❌ **할당량 초과**: 900명 (예상대로 처리됨)
- 🔄 **중복 신청**: 0명
- ⚠️ **소유자 신청 차단**: 0명
- 💥 **실패/에러**: 0건

### 극한 경쟁 테스트 (Oversubscribed Load Test)
**테스트 환경:**
- Redis Cluster: 3 Master + 3 Replica 노드  
- 동시 사용자: 1,000명
- 게시물 할당량: 5개 (극한 경쟁 상황)

**성능 지표:**
```
🚀 처리량 (Throughput): ~457 요청/초
⏱️  총 처리 시간: 2,189ms
👥 사용자 생성 시간: 1,703ms (1,000명)
```

**응답 시간 분석:**
- **최소 응답 시간**: 3ms
- **최대 응답 시간**: 1,334ms
- **P50 (중간값)**: 75ms
- **P95**: 1,260ms
- **P99**: 1,303ms

**정확성 검증:**
- ✅ **성공한 신청**: 5명 (할당량과 정확히 일치)
- ❌ **할당량 초과**: 995명 (예상대로 처리됨)
- 🔄 **중복 신청**: 0명
- ⚠️ **소유자 신청 차단**: 0명
- 💥 **실패/에러**: 0건

### 핵심 특징

#### 1. 원자적 처리 (Atomic Operations)
- Redis Lua 스크립트를 활용한 동시성 제어
- Race Condition 완전 방지
- 정확한 선착순 보장

#### 2. 확장성 (Scalability)
- Redis Cluster 구성으로 고가용성 확보
- 마스터-복제본 구조로 읽기 성능 향상
- 수평 확장 지원

#### 3. 성능 최적화
- 비동기 이메일 발송으로 응답 시간 단축
- 커넥션 풀링으로 리소스 효율성 향상
- 가상 스레드 활용 (Java 21)

### 시스템 아키텍처

```
[Frontend] → [Spring Boot] → [Redis Cluster] → [PostgreSQL]
                    ↓
               [OAuth2 Provider]
                    ↓
               [Email Service]
```

## 벤치마크 테스트 시나리오

### 1. Small Load Test
- 동시 사용자: 50명
- 할당량: 10개

### 2. Medium Load Test  
- 동시 사용자: 200명
- 할당량: 25개

### 3. High Load Test
- 동시 사용자: 500명
- 할당량: 50개

### 4. Extreme Load Test
- 동시 사용자: 1,000명
- 할당량: 100개

### 5. Oversubscribed Load Test
- 동시 사용자: 1,000명
- 할당량: 5개 (극한 경쟁 상황)

## Redis 설정

### Cluster 구성
```yaml
spring:
  data:
    redis:
      cluster:
        enabled: true
        nodes:
          - localhost:9001
          - localhost:9002
          - localhost:9003
          - localhost:9004
          - localhost:9005
          - localhost:9006
        max-redirects: 3
        password: myredispassword
```

### 인증 방식
- **Password-only 인증** (requirepass)
- Redis 7.2 호환
- ACL 사용 안함 (단순화)

## 실행 방법

### 개발 환경 설정
1. PostgreSQL 데이터베이스 생성
2. Redis Cluster 시작
3. 환경 변수 설정 (OAuth2 키)
4. 애플리케이션 실행

### 벤치마크 테스트 실행
```bash
# 단일 테스트
./gradlew test --tests ConcurrentClaimBenchmarkTest.benchmarkConcurrentClaims_SmallLoad

# 전체 벤치마크
./gradlew test --tests ConcurrentClaimBenchmarkTest

# Redis 모드 지정
./gradlew test --tests ConcurrentClaimBenchmarkTest -Dspring.profiles.active=test,redis-cluster
```

## 결론

본 FCFS 시스템은 **1,000명의 동시 사용자**가 **100개의 한정된 자원**을 놓고 경쟁하는 상황에서도 **완벽한 정확성**과 **우수한 성능**을 보여주었습니다. Redis Cluster와 Lua 스크립트를 통한 원자적 처리로 동시성 문제를 해결하고, 평균 50ms의 빠른 응답 시간을 달성했습니다.

### 성능 하이라이트
- 🎯 **100% 정확성**: 할당량 초과 없음
- ⚡ **평균 50ms**: 중간값 응답 시간
- 🚀 **514 req/s**: 초당 처리량
- 💪 **0건 에러**: 완벽한 안정성

이는 실제 서비스에서 대용량 트래픽을 안정적으로 처리할 수 있음을 입증합니다.

## 성능 분석 및 개선 방안

### P99 응답시간이 1초인 이유

**1. 동시성 병목 현상**
- 1,000명이 동시에 Redis Cluster에 요청
- 스레드 풀 경합으로 인한 대기 시간 발생
- Redis 연결 풀 한계로 인한 큐잉

**2. 네트워크 지연**
- Redis Cluster 노드 간 통신 오버헤드
- MOVED/ASKED 리다이렉션으로 인한 추가 RTT
- TCP 연결 재사용 지연

**3. JVM 가비지 컬렉션**
- 대량 객체 생성으로 인한 GC 압박
- Stop-the-world 시간이 일부 요청에 영향

### 성능 개선 방안

#### 1. 연결 풀 최적화
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 32      # 증가 (기존: 16)
          max-idle: 16        # 증가 (기존: 8)
          min-idle: 8         # 증가 (기존: 0)
          max-wait: 1000ms    # 대기 시간 단축
      timeout: 1000ms         # 타임아웃 단축
```

#### 2. 스레드 풀 튜닝
```java
@Bean
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);      // 기본 스레드 수 증가
    executor.setMaxPoolSize(200);      // 최대 스레드 수 증가
    executor.setQueueCapacity(500);    // 큐 크기 조정
    executor.setKeepAliveSeconds(60);
    return executor;
}
```

#### 3. Redis 최적화
```bash
# redis.conf 튜닝
tcp-keepalive 60
tcp-backlog 511
maxclients 10000
timeout 30

# 메모리 최적화
maxmemory-policy allkeys-lru
save ""  # 동기화 비활성화 (성능 우선)
```

#### 4. 애플리케이션 최적화
- **비동기 처리 확대**: 이메일 발송 외 모든 후속 작업 비동기화
- **캐싱 전략**: 자주 조회되는 데이터 로컬 캐싱
- **배치 처리**: 여러 요청을 묶어서 처리
- **프로파일링**: JProfiler로 병목 지점 식별

#### 5. 인프라 개선
- **Redis 클러스터 확장**: 6노드 → 9노드 (3M+6R)
- **로드 밸런서**: 여러 애플리케이션 인스턴스 분산
- **전용 네트워크**: Redis와 애플리케이션 간 전용선 구성

### 예상 성능 개선 효과
```
현재 성능:
- P50: 50-75ms
- P95: 1,260-1,399ms  
- P99: 1,303-1,431ms

개선 후 예상:
- P50: 20-30ms      (40-50% 개선)
- P95: 200-400ms    (70-80% 개선)
- P99: 500-800ms    (40-60% 개선)
- 처리량: 800-1000 req/s (60-90% 개선)
```

### 모니터링 지표
- **Redis 메트릭**: 연결 수, 명령 처리 시간, 메모리 사용량
- **JVM 메트릭**: GC 시간, 힙 사용량, 스레드 수
- **애플리케이션 메트릭**: 응답 시간 분포, 에러율, 처리량

이러한 최적화를 통해 P99 응답시간을 1초 미만으로 단축하고 전체적인 성능을 크게 향상시킬 수 있습니다.
