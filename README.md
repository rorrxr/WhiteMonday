# WhiteMonday - 쇼핑몰 핫딜 서비스


## 프로젝트 소개
- WhiteMonday는 빠르게 변화하는 e-commerce 환경에서 효율적인 대규모 주문 처리와 선착순 구매 기능을 제공하는 쇼핑몰 핫딜 서비스입니다.
- Redis 기반의 캐싱과 마이크로서비스 아키텍처(MSA)를 도입하여 트래픽이 몰리는 상황에서도 안정적인 사용자 경험을 목표로 합니다.
---

## 프로젝트 목표
- 선착순 구매 환경 최적화: 대규모 트래픽에서의 주문과 재고 관리 최적화
- 확장 가능한 아키텍처: 기존 모노리스 서비스를 MSA로 전환
- 실시간 성능 개선: Redis를 활용한 주문 처리와 재고 관리를 통해 실시간 응답 속도 향상
- 자동화된 테스트와 복구 탄력성: 장애 상황에서도 시스템 안정성을 보장
---
## 주요 기능

1. 사용자 관리

- 이메일 인증 기반 회원가입
- 개인정보 암호화 저장
- JWT 기반 로그인 및 로그아웃 기능

2. 상품 관리

- 상품 목록 및 상세 페이지 제공
- 선착순 구매 상품 및 일반 상품 구분
- 상품 재고 실시간 업데이트

3. 주문 관리

- 위시리스트 추가 및 조회
- 주문 및 반품 관리
- 주문 상태 실시간 추적 (결제 중, 배송 중, 배송 완료 등)

4. 결제 프로세스

---

## 기술 스택
- Backend : Spring Boot 3.x, JAVA 21, Spring Security, Spring Data JPA
- Database : MySQL, Redis, DBeaver
- Infra : Docker, AWS
- CI/CD :
- Version Control : Git
---

- 프로젝트에 사용된 기술 스택에는 무엇이 있나요?
- 왜 해당 스택을 선택하였는지, **기술적 의사결정**에 대해서도 함께 작성해 주세요.
    - 고려한 기술, 선택한 기술, 선택한 이유

## API & ERD

---

## 성능 개선

---

## 트러블 슈팅


