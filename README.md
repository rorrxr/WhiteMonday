# WhiteMonday - 선착순 구매 e-commerce

## **📖 목차**

1. [🔎 프로젝트 소개](#-프로젝트-소개)
2. [🎯 프로젝트 기간](#-프로젝트-기간)
3. [🚀 주요 기능](#-주요-기능)
5. [❓ 기술적 의사결정](#-기술적-의사결정)
3. [📃 API 설계서](#-api-설계서)
6. [🛠 ERD](#-erd)
7. [🛠 아키텍처](#-아키텍처)
8. [🚨 트러블 슈팅](#-트러블-슈팅)

## 🔎 프로젝트 소개

- WhiteMonday는 빠르게 변화하는 e-commerce 환경에서 효율적인 대규모 주문 처리와 선착순 구매 기능을 제공하는 서비스입니다.
- 마이크로서비스 아키텍처(MSA)를 도입하여 트래픽이 몰리는 상황에서도 안정적인 사용자 경험을 목표로 합니다.

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
- MVP 개발 기간 : 2024.12.18 ~ 2025.01.31
- 프로젝트 인원 : 1인 (개인)

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
- Backend : JAVA 21, Spring Boot 3.3.6, Spring Security, Spring Data JPA, Spring Cloud Netflix Eureka Client, JWT (Authentication & Token Management), Lombok
- Database : MySQL, Redis, DBeaver
- Test Tool : Postman, K6
- DevOps : Docker, Git
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

## 📃 API 설계서

[🔗 API 설계서](https://documenter.getpostman.com/view/39655317/2sAYX2N4Lg)

## 🛠 ERD

![erd.png](image%2Ferd.png)

## 🛠 아키텍처

![시스템 아키텍처.drawio.png](image%2F%EC%8B%9C%EC%8A%A4%ED%85%9C%20%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98.drawio.png)

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

### 3️⃣ 재고 초과 결제 문제 해결

**[BEFORE] 재고 초과 결제 발생**

- **문제**: 다수 사용자가 동시에 결제 요청을 보낼 때, `최종 결제 수 > 전체 상품 수` 상황이 발생.
- **현상**:
    - 결제가 완료된 후 재고 초과가 확인되어 뒤늦게 취소 처리.
    - 일부 사용자에게 결제 실패 또는 취소가 반복적으로 발생.
- 결제가 완료된 후 재고 초과가 확인되어 뒤늦게 취소 처리.
- 일부 사용자에게 결제 실패 또는 취소가 반복적으로 발생.
- **테스트 결과**:
    - **결제 실패율**: 23%

**[AFTER] Redis 기반 재고 관리로 초과 결제 방지**

- **해결**:
    - Redis에서 재고 감소 및 검증 로직을 처리하여 데이터베이스 충돌 방지.
    - 결제 프로세스 단계(결제 화면, 결제 중, 결제 완료)를 상태별로 관리하여 `전체 상품 수 - 결제 프로세스 도입 수`를 실시간 반영.
    - `Redisson`의 분산 락을 사용해 재고 감소를 동기화하여 초과 감소 방지.
- Redis에서 재고 감소 및 검증 로직을 처리하여 데이터베이스 충돌 방지.
- 결제 프로세스 단계(결제 화면, 결제 중, 결제 완료)를 상태별로 관리하여 `전체 상품 수 - 결제 프로세스 도입 수`를 실시간 반영.
- `Redisson`의 분산 락을 사용해 재고 감소를 동기화하여 초과 감소 방지.
- **테스트 결과**:
    - **결제 실패율**: 7.12%
    - **TPS**: 평균 508건/초, 최대 5760ms의 지연 발생