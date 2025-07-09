# WhiteMonday - 선착순 구매 e-commerce
## 📖 목차

1. [🔎 프로젝트 소개](#-프로젝트-소개)
2. [🎯 프로젝트 기간 및 인원](#-프로젝트-기간-및-인원)
3. [📁 프로젝트 실행 방법](#-프로젝트-실행-방법)
4. [🛠 아키텍처](#-아키텍처)
5. [🚀 주요 기능](#-주요-기능)
6. [💻 기술 스택](#-기술-스택)
7. [❓ 기술적 의사결정](#-기술적-의사결정)
8. [📃 API 설계서](#-api-설계서)
9. [🚨 트러블 슈팅](#-트러블-슈팅)
10. [✅ 프로젝트 향후 개선 방안](#-프로젝트-향후-개선-방안)

## 🔎 프로젝트 소개



WhiteMonday는 빠르게 변화하는 e-commerce 환경에서 **효율적인 대규모 주문 처리**와 **선착순 구매 기능**을 제공하는 서비스입니다.  
마이크로서비스 아키텍처(MSA)를 도입하여 **트래픽이 집중되는 상황**에서도 안정적인 사용자 경험을 목표로 합니다.


<details>
  <summary>🎇 프로젝트 실행 방법</summary>

### 1️⃣ Git Clone
  ```bash
  git clone https://github.com/rorrxr/WhiteMonday.git
```

### 2️⃣ .env 파일 설정

```
# Docker
MYSQL_ROOT_PASSWORD={데이터베이스 비밀번호}
MYSQL_DATABASE={데이터베이스 이름}

DB_USERNAME={데이터베이스 username}
DB_PASSWORD={username의 비밀번호}

# SMTP
MAIL_USERNAME={SMTP 메일 송신 이메일}
MAIL_PASSWORD={SMTP 메일 송신 비밀번호}

# JWT Secret Key
JWT_SECRET_KEY={JWT KEY값}

# Encryption Secret Key
ENCRYPTION_SECRET_KEY={암호화 KEY값}

# 스프링 데이터베이스 URL
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/{데이터베이스 이름}?useSSL=false&allowPublicKeyRetrieval=true
```

### 3️⃣ Docker 이미지 빌드
``` bash
docker buildx build --platform linux/amd64 -f eureka-server/Dockerfile -t eureka-server:latest . --load
docker buildx build --platform linux/amd64 -f gateway-service/Dockerfile -t gateway-service:latest . --load
docker buildx build --platform linux/amd64 -f product-service/Dockerfile -t product-service:latest . --load
docker buildx build --platform linux/amd64 -f user-service/Dockerfile -t user-service:latest . --load
docker buildx build --platform linux/amd64 -f order-service/Dockerfile -t order-service:latest . --load
docker buildx build --platform linux/amd64 -f wishlist-service/Dockerfile -t wishlist-service:latest . --load
docker buildx build --platform linux/amd64 -f payment-service/Dockerfile -t payment-service:latest . --load
```

### 4️⃣ Docker Compose로 컨테이너 실행
```bash
docker-compose up --build -d
```    
</details>


## 🎯 프로젝트 기간
- 1차 MVP 개발 기간 : 2024.12.18 ~ 2025.01.31
- 2차 MVP 개발 기간 : 2025.05.15 ~ 2025.06.30
- 프로젝트 인원 : 1인 (개인)

## 🛠 아키텍처


## 🚀 주요 기능
### 📌 사용자 관리
- Google SMTP를 사용한 이메일 인증 기반 회원가입 기능
- AES 알고리즘을 사용하여 개인정보 암호화 저장
- JWT, Spring Security 기반으로 하여 로그인 및 로그아웃 기능
### 📌 상품 관리
- 상품 목록 및 상세 페이지 제공
- 선착순 구매 상품 및 일반 상품 구분
- 상품 재고 실시간 업데이트
- 위시리스트 조회 기능
### 📌 주문 관리
- 주문 및 반품 관리
- 주문 상태 실시간 추적 (결제 중, 배송 중, 배송 완료 등)
- 결제 진입 및 결제 기능

## 💻 기술 스택
- Backend : JAVA 21, Spring Boot 3.3.6, Spring Security, Spring Data JPA, Spring Cloud Netflix Eureka Client, JWT, CircuitBreaker
- Database : MySQL, Redis, DBeaver, Kafka
- Test Tool : Postman, K6, JUnit
- DevOps : Docker, Git, Prometheus, Grafana
- ETC : InteliJ, Google SMTP


## ❓ 기술적 의사결정

<details>
<summary> 1️⃣ MSA 도입 계기</summary>
    WhiteMonday 프로젝트에서 마이크로서비스 아키텍처(MSA)를 도입한 이유는 e-commerce 환경에서 요구되는 확장성과 안정성을 확보하기 위함입니다. MSA는 서비스 단위를 독립적으로 설계하고 운영할 수 있어, 특정 서비스의 변경이나 장애가 발생해도 다른 서비스에 미치는 영향을 최소화할 수 있습니다.
    <br><br>
    특히, 핫딜 이벤트와 같이 트래픽이 급증하는 상황에서 MSA의 개별 서비스 확장 기능이 큰 이점을 제공합니다. 예를 들어, 상품 관리 서비스에 트래픽이 집중되는 경우 해당 서비스만 수평 확장을 통해 대응함으로써 전체 시스템의 안정성을 유지할 수 있었습니다.
    <br><br>
    또한, MSA는 장애 격리에 유리하여 특정 서비스에서 문제가 발생하더라도 나머지 서비스는 정상적으로 동작할 수 있어 사용자 경험을 보호할 수 있습니다. 이러한 구조는 대규모 트래픽과 복잡한 비즈니스 로직이 요구되는 e-commerce 프로젝트에 특히 적합합니다.
    <br><br>
    결론적으로 MSA를 통해 확장성과 안정성을 동시에 확보하며, 복잡한 비즈니스 요구사항을 충족하는 아키텍처를 구축하고 있습니다.
</details>

<details>
  <summary> 2️⃣ Spring Boot</summary>
  Spring Boot는 생산성, 확장성, 그리고 Spring 생태계와의 통합성 측면에서 우수하여 주요 프레임워크로 선정되었습니다. Spring Boot의 자동 설정 기능과 내장 서버는 초기 개발 과정을 단순화하고 빠른 프로토타이핑을 가능하게 했습니다.
    <br><br>
    또한, Spring Framework의 풍부한 기능을 활용해 MSA 환경에서 마이크로서비스 간 통신(예: Feign Client)이나 분산 시스템 구현을 간소화했습니다. 높은 유연성을 제공함으로써 프로젝트 요구사항의 변화에도 쉽게 대응할 수 있었으며, 커뮤니티와 문서 지원을 통해 개발 중 발생한 문제를 신속히 해결할 수 있었습니다.
    <br><br>
    이와 함께, 지속적인 업데이트와 보안 패치 제공을 통해 안정성과 신뢰성을 보장할 수 있어 Spring Boot를 프로젝트의 핵심 프레임워크로 채택했습니다.
</details>

<details>

  <summary> 3️⃣ Spring Boot 3.3.6과 JAVA 21 버전 선택</summary>
      Spring Boot 3.3.6과 Java 21의 조합은 프로젝트에서 발생했던 호환성 문제를 해결하기 위해 선택되었습니다.
    <br><br>
    초기 개발 과정에서 Feign Client 통신 및 일부 Spring Boot 기능이 예상대로 동작하지 않는 문제가 있었으며, 이는 사용 중인 Spring Boot와 Java 버전 간의 호환성 문제로 확인되었습니다. Spring Boot 3.3.6은 최신 LTS(Lifecycle Support)를 지원하며, Java 21은 Long-Term Support 버전으로 최신 기능과 안정성을 제공합니다. 두 버전 간 상호 호환성이 보장될 뿐만 아니라 성능 최적화와 보안 패치 측면에서도 우수한 선택이었습니다.
    <br><br>
    이를 통해 Feign Client 통신 문제를 비롯한 여러 기술적 문제를 해결할 수 있었으며, 안정성과 성능 모두를 확보한 아키텍처를 구현할 수 있었습니다.
</details>

<details>
  <summary> 4️⃣ MySQL (RDBMS)</summary>

MySQL은 데이터의 일관성을 유지하고 핵심 데이터를 안정적으로 관리하기 위해 선택되었습니다. <br><br>
상품 정보와 주문 데이터를 정규화된 테이블 구조로 관리함으로써 데이터 무결성을 보장하였으며, JPA를 통해 데이터 관리와 비즈니스 로직을 효율적으로 처리할 수 있었습니다.<br><br>
또한, 검증된 안정성과 데이터 보존 및 백업 용이성을 바탕으로 주요 데이터를 안전하게 저장하고 유지할 수 있었습니다.
</details>


<details>

  <summary> 5️⃣ Redis (NoSQL)</summary>
  Redis는 실시간 데이터 캐싱을 통해 시스템 성능을 최적화하기 위해 도입되었습니다. 
    <br><br>
    대규모 트래픽 상황에서 자주 조회되는 재고 정보를 캐싱하여 데이터베이스 접근을 줄이고 빠른 응답 속도를 보장할 수 있었습니다. 
    <br><br>
    또한, Redis의 분산 락 기능을 활용해 동시성 제어 문제를 해결하였으며, 간단한 구현과 뛰어난 성능 덕분에 효율적으로 동시성 문제를 처리할 수 있었습니다. Redis는 읽기 요청을 처리하고 MySQL은 쓰기 작업을 담당하도록 설계하여, 데이터 일관성과 성능의 균형을 유지했습니다.
</details>

<details>
  <summary> 6️⃣ MySQL (RDBMS)과 Redis (NoSQL) 병행 사용 선택</summary>

MySQL과 Redis를 병행 사용한 이유는 각각의 장점을 살리면서 데이터 일관성과 성능을 동시에 확보하기 위함입니다.

MySQL은 주문 및 결제와 같은 핵심 데이터를 안전하게 관리하며, Redis는 빠른 데이터 조회와 캐싱을 통해 높은 트래픽에도 안정적인 성능을 제공할 수 있었습니다. 이 설계를 통해 대규모 트래픽 상황에서도 안정성과 확장성을 모두 달성할 수 있었습니다.
</details>

<details>
    <summary> 7️⃣ Feign Client</summary>
    Feign Client는 간결하고 직관적인 선언적 API 클라이언트를 제공하여 코드의 가독성과 유지보수를 크게 개선했습니다. 
    <br><br>
    이를 통해 서비스 간 통신을 효율적으로 처리할 수 있었으며, MSA 환경에서 반복적으로 발생하는 통신 관련 로직을 간소화할 수 있었습니다.
</details>

<details>
      <summary> 8️⃣ K6</summary>
    k6는 성능 테스트의 간편한 구현과 효율적인 리소스 사용을 위해 선택되었습니다. JavaScript 기반의 직관적인 스크립트 작성과 간단한 CLI 환경은 초기 학습 비용을 줄이고 빠르게 테스트를 시작할 수 있도록 도와줍니다.
    <br><br>
    고성능 C++ 기반으로 개발된 k6는 동시 사용자(VU) 시뮬레이션에 최적화되어 있어 대규모 테스트 환경에서도 안정적으로 동작하며, Prometheus 및 Grafana와 같은 모니터링 도구와의 통합을 통해 실시간 데이터 시각화와 분석이 가능합니다.
    <br><br>
    또한, 사용자 정의가 용이하여 복잡한 테스트 시나리오를 구현할 수 있었으며, 오픈소스 기반으로 커뮤니티 지원과 지속적인 업데이트를 통해 최적의 성능 테스트 환경을 구축할 수 있었습니다.
</details>

<details>
      <summary> 9️⃣ Kafka </summary>
  Kafka와 SAGA 패턴은 주문 → 결제 → 재고 반영 과정에서 발생하는 동기 처리의 성능 병목과 트랜잭션 관리 문제를 해결하기 위해 도입되었습니다. <br>

WhiteMonday 프로젝트에서는 사용자의 주문 요청이 들어오면, 결제 승인과 재고 차감이 순차적으로 이루어지는 구조였습니다. 이 과정에서 하나의 단계에서 지연이 발생하면 전체 프로세스가 지연되는 문제가 발생했고, 특히 결제 승인 지연 또는 재고 서비스의 응답 지연으로 인해 사용자 응답 시간이 비정상적으로 늘어나는 성능 병목이 발생했습니다.<br>

또한, 각 서비스가 독립적인 DB를 사용하는 MSA 구조에서 단일 트랜잭션으로 묶어 처리할 수 없기 때문에 데이터 일관성 보장이 어려운 상황이었습니다.<br>

Kafka 기반 비동기 메시징 도입과 SAGA 패턴 적용을 통한 분산 트랜잭션 보상 처리를 적용하여 전체 요청 처리 시간을 동기 구조 대비 대폭 단축시키고, 서비스 간의 느슨한 결합을 유지하면서도 데이터 정합성 확보와 대용량 주문 요청에 대해 확장성과 장애 대응력 확보하였습니다.
<br>
</details>

<details>
      <summary> 1️⃣0️⃣ Prometheus와 Grafana </summary>
  Prometheus와 Grafana는 MSA 환경에서 실시간 모니터링과 장애 대응 체계를 구축하기 위해 도입되었습니다.<br><br>

WhiteMonday 프로젝트는 다수의 마이크로서비스가 상호작용하는 구조로, 특정 서비스의 장애가 연쇄적으로 다른 서비스에 영향을 미칠 수 있는 리스크가 존재했습니다. 이에 따라 **문제가 발생하기 전에 사전 징후를 탐지하고, 실시간으로 시스템 상태를 시각화하여 빠르게 대응**할 수 있는 모니터링 시스템의 필요성이 대두되었습니다. <br><br>

Prometheus는 시계열 기반의 메트릭 수집 도구로, 애플리케이션의 다양한 지표(요청 수, 응답 시간, 에러율 등)를 정기적으로 수집하고 저장합니다. Grafana는 이러한 데이터를 시각화하는 대시보드 도구로, 서비스의 상태를 직관적으로 확인할 수 있도록 도와줍니다.
<br>
</details>

<details>
      <summary> 1️⃣1️⃣ CircuitBreaker </summary>
MSA 환경에서 특정 서비스가 일시적으로 장애를 겪거나 응답 지연이 발생할 경우 전체 흐름이 중단되는 문제가 존재했습니다. <br>

이를 해결하기 위해 Resilience4j의 Circuit Breaker와 Retry 기능을 도입하여 장애 대응 체계를 구축하였습니다. <br>

일시적인 장애나 과부하로 인해 특정 서비스의 실패율이 증가할 경우, 해당 서비스에 대한 호출을 차단하고 fallback 로직으로 우회 처리하도록 설계하였습니다. <br>

이를 통해 실패한 서비스에 불필요한 호출을 반복하지 않음으로써, 시스템 자원 낭비를 방지하고 전체 서비스 장애 확산을 차단할 수 있었습니다.<br>

반대로, 네트워크 일시적 불안정 또는 순간적인 서비스 오류와 같이 일회성 장애는 재시도를 통해 정상 처리가 가능한 상황이 많았습니다. <br>

이러한 경우를 고려하여 일정 횟수 자동 재시도 후 처리되도록 @Retry 설정을 적용하였고, 재시도 후에도 실패할 경우에는 fallback 함수로 graceful하게 실패 처리하도록 구성하였습니다. <br>

결국 Circuit Breaker는 지속적인 장애에 대한 보호, Retry는 일시적인 장애에 대한 회복을 목적으로 선택하였으며, 서비스 간 연쇄 장애 방지 및 복원력 확보와 사용자 응답 실패율 감소 서비스 안정성 향상 및 예외 상황 대응 자동화를 위해 선택하게 되었습니다.
<br>
</details>

## 📃 API 설계서

[🔗 API 설계서](https://documenter.getpostman.com/view/39655317/2sAYX2N4Lg)

## 🚨 트러블 슈팅

### 1️⃣ 실시간 재고 캐싱 처리

**[BEFORE] DB 직접 조회**

- **문제**: 재고 데이터를 데이터베이스에서 직접 조회하면서 트래픽이 많아질 경우 성능 저하 발생.
- **현상**: 데이터베이스 I/O 부하로 인해 응답 시간이 길어짐.
- **테스트 결과**:
  - **TPS**: 182.85/sec
  - **평균 요청 응답 시간**: 1.27s

**[AFTER] Redis 캐싱 처리**

- **해결**:
  - `RedisTemplate`을 사용해 Cache-Aside 전략으로 데이터 캐싱.
  - 조회 요청 시 Redis에서 캐싱된 데이터를 반환해 DB 조회 빈도를 감소.
  - 초기 상품 등록 시 Redis에 재고를 저장하여 빠른 조회가 가능하도록 구성.
- **테스트 결과**:
  - **TPS**: 503.35/sec (175% 증가)
  - **평균 요청 응답 시간**: 133.1ms (89.5% 감소)

---

### 2️⃣ 동시성 문제 해결

**[BEFORE] 동시 재고 업데이트 충돌 → DB 락 기반 처리**

- **문제**: 다수의 사용자가 동시에 재고를 업데이트할 경우 동시성 충돌 발생
- **현상**: DB 락 기반 동시성 처리로 인해 성능 저하 및 데드락 가능성
- **영향**: 대규모 트래픽 상황에서 재고 업데이트 실패 확률 증가

**[AFTER] Redis 기반 분산 락 적용**

- **해결**:
  - `Redisson`의 `RLock`을 사용해 재고 감소 시 분산 락 적용
  - 한 번에 한 사용자가 특정 상품의 재고를 수정하도록 제어
  - TTL(Time-to-Live) 설정을 통해 데드락 방지
- **결과**:
  - 동시성 충돌 없음
  - 데이터 일관성 보장
  - 대규모 트래픽 상황에서도 안정적으로 재고 업데이트 가능

---

### 3️⃣ 장애 대응 문제 해결 : Circuit Breaker, Retry, Prometheus/Grafana

**[BEFORE] 장애 전파 및 일시적 오류에 취약한 구조**

- **문제**: 주문 → 결제 → 재고 처리 흐름에서 하나의 서비스가 장애 나면 전체 프로세스가 중단됨.
- **현상**:
  - 예: 결제 서비스 지연 시 전체 주문 API 지연.
  - 일시적 장애에도 사용자에게 실패 응답 발생.
- **영향**:
  - 사용자 경험 저하.
  - 장애가 연쇄적으로 확산될 가능성.
  - 장애 원인 파악 어려움.

**[AFTER] Circuit Breaker + Retry + Prometheus/Grafana 도입**

- **해결**:
  - **Resilience4j의 CircuitBreaker**:
    - 외부 서비스 호출 실패율이 일정 이상이면 자동으로 회로 차단하여 연쇄 장애 방지.
    - 장애 시 Fallback 메서드 호출로 graceful degradation 처리.
  - **Resilience4j의 Retry**:
    - 일시적인 네트워크 오류/예외 발생 시 자동 재시도 설정.
    - 재시도 횟수 및 대기 시간 지정 가능.
  - **Prometheus + Grafana**:
    - CircuitBreaker 상태, 요청 실패율, 서비스별 응답 시간 등을 실시간으로 시각화.
    - 알림 설정을 통해 장애 조기 감지 가능.
- **결과**:
  - 장애 전파 방지 및 유연한 예외 처리 구조 확보.
  - 서비스 안정성 향상.
  - 실시간 장애 모니터링 및 빠른 대응 체계 구축.

---

### 4️⃣ Kafka 기반 비동기 트랜잭션 처리

**[BEFORE] 동기 호출 기반 트랜잭션 처리**

- **문제**: 주문 → 결제 → 재고 감소가 순차적 동기 방식으로 호출됨.
- **현상**:
  - 예: 결제 서비스 지연 시 전체 주문 API 지연.
  - 일시적 장애에도 사용자에게 실패 응답 발생.
- **영향**:
  - 사용자 경험 저하.
  - 장애가 연쇄적으로 확산될 가능성.
  - 장애 원인 파악 어려움.

**[AFTER] Kafka 이벤트 드리븐 구조로 전환**

- **해결**:
  - **Kafka + Saga 패턴 적용**:
    - 주문 생성 시 Kafka의 order-created-topic에 이벤트 발행.
    - 결제 서비스는 해당 이벤트를 비동기적으로 수신 후 결제 처리.
    - 결제 성공/실패 결과를 다시 이벤트로 발행하여 이후 단계(재고 감소 또는 보상 처리) 진행.

  - 각 단계는 이벤트 기반 비동기 처리로 연결되어 서비스 간 결합도를 낮춤.
  - 보상 트랜잭션 구조로 실패 시 재고 복원 등 대응 가능.

- **결과**:
  - 각 서비스 간 느슨한 결합 → 장애 전파 최소화.
  - 대규모 트래픽 상황에서도 안정적인 트랜잭션 처리.
  - 확장성과 유연성 확보.
--- 
## ✅ 프로젝트 향후 개선 방안

### 1. 🔍 QueryDSL 도입으로 쿼리 최적화 및 유지보수성 향상

- **[현재]**  
  복잡한 조건이나 동적 검색 쿼리를 JPQL 또는 Native Query로 직접 작성하고 있음

- **[이후]**  
  QueryDSL을 도입하여 가독성 높은 동적 쿼리 구성 및 재사용 가능한 조건 빌더 패턴 적용 예정

- **[기대효과]**
  - JPA의 정적 쿼리 한계를 보완하고, 유지보수성과 테스트 편의성 향상
  - 서비스별 공통 조건/정렬 처리 추상화로 재사용성 증가

---

### 2. 🔐 Lua 스크립트 기반 Redis 락 처리로 동시성 제어 정교화

- **[현재]**  
  Redisson의 `RLock`을 사용하여 상품 재고 차감 등의 동시성 제어 처리

- **[이후]**  
  Redis Lua 스크립트를 활용한 원자적 락 처리 방식으로 전환 예정

- **[기대효과]**
  - 락 획득/해제의 일관성 강화 및 예외 상황 제어 정밀화
  - 네트워크 오버헤드 감소로 성능 개선

---

### 3. 🚀 Jenkins + Kubernetes 기반의 CI/CD 및 운영환경 고도화

- **[현재]**  
  Docker Compose를 활용한 로컬 중심 배포 및 수동 빌드 과정

- **[이후]**  
  Jenkins 기반 CI/CD 파이프라인 구성 후, Kubernetes 환경으로 클러스터 배포 전환 예정

- **[기대효과]**
  - 자동화된 테스트 및 배포로 신속한 배포 사이클 확보
  - 롤링 업데이트, Canary 배포, 오토스케일링 등 실무형 인프라 실습 가능

---

### 4. 📡 Zipkin 기반 분산 추적 및 장애 발생 위치 시각화

- **[현재]**  
  서비스 간 호출 흐름 추적 어려움 → 로그를 수동으로 추적 중

- **[이후]**  
  Spring Sleuth + Zipkin을 연동하여 호출 흐름 시각화 및 추적 시스템 구축

- **[기대효과]**
  - 장애 발생 위치 및 전파 지점을 빠르게 파악 가능
  - 호출 병목 구간 최적화 기반 마련

---

### 5. 📈 Prometheus + Grafana 기반 실시간 모니터링 시스템 고도화

- **[현재]**  
  기본 로그 수준의 단일 서비스 모니터링만 가능

- **[이후]**  
  Prometheus + Grafana 기반 대시보드를 구성하고, AlertManager를 통한 알림 체계 구축 예정

- **[기대효과]**
  - 주요 메트릭 실시간 시각화 및 서비스 이상 조기 탐지
  - Slack, Email 등과 연동한 자동 알림 시스템 구현

---

### 6. 📦 Elasticsearch 기반 고성능 검색 및 로그 분석

- **[현재]**  
  상품 검색 기능은 RDB 기반으로만 처리, 로그 분석은 수동 탐색

- **[이후]**  
  Elasticsearch 기반 상품 검색 API 구축 및 ELK 파이프라인 구성(Logstash → Elasticsearch → Kibana)

- **[기대효과]**
  - 빠른 검색 응답 및 복합 필터링 제공
  - 로그의 시계열 분석 및 트렌드 모니터링 가능

---

### 7. 🌐 API Gateway 기능 확장으로 안정적 서비스 제공

- **[현재]**  
  Spring Cloud Gateway는 라우팅 및 인증 필터 기능만 적용된 상태

- **[이후]**  
  Redis 기반 Rate Limiting, 응답 캐싱, API 버전 관리 기능 추가 예정

- **[기대효과]**
  - 과도한 요청 차단 및 트래픽 보호
  - 신규 기능 버전 롤아웃 시 서비스 안정성 유지
  - 캐시 적용을 통한 전체 시스템 부하 감소

---

### 8. 🔐 OAuth2 기반 인증/인가 시스템 고도화

- **[현재]**  
  JWT 기반 인증을 Gateway 단에서 직접 처리

- **[이후]**  
  Spring Security + OAuth2 Resource Server 조합으로 역할 기반 인가 체계 구현 예정

- **[기대효과]**
  - Scope 기반 접근 제어 및 관리자/사용자 권한 분리
  - 민감 API 보호 및 보안 로그 분석 체계 수립

---

### 9. 🔁 Kafka Topic 기반 장애 대응 및 메시지 재처리

- **[현재]**  
  Kafka 기반 이벤트 흐름만 구성되어 있으며, 실패 메시지에 대한 재처리 구조 없음

- **[이후]**  
  Dead Letter Topic(DLT) 구성 및 메시지 재시도 로직(RetryTemplate 또는 Kafka backoff) 도입 예정

- **[기대효과]**
  - 장애 시 유실 없는 메시지 재처리 가능
  - 실패 이벤트에 대한 Slack 알림 및 관리자 API 대응체계 구성

---

