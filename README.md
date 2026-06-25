# Toss Payments Toy Project

Toss Payments API 연동을 연습하기 위해 만든 Spring Boot 기반 결제 토이 프로젝트입니다.  
주문 생성, 결제 승인, 결제 취소, 결제 조회, 웹훅 수신, 빌링키 기반 자동결제 흐름을 간단한 REST API로 구현했습니다.

## 프로젝트 개요

- Toss Payments 결제 승인 API 연동
- 주문 금액 사전 저장 및 결제 승인 시 금액 검증
- 결제 취소 및 부분 취소 요청
- paymentKey, orderId 기반 결제 조회
- 결제 내역 DB 저장
- Toss Payments 웹훅 수신
- 빌링키 발급 및 자동결제 요청
- H2 개발 DB 및 MySQL 운영 DB 프로필 분리

## 기술 스택

- Java 17
- Spring Boot 4.0.5
- Spring Web
- Spring Validation
- Spring Data JPA
- Spring WebFlux WebClient
- H2 Database
- MySQL Driver
- Gradle Kotlin DSL
- Lombok

## 프로젝트 구조

```text
src/main/java/com/example/tosspayments
├── billing
│   ├── application
│   └── presentation
├── global
│   ├── client
│   ├── config
│   └── exception
├── order
│   ├── application
│   ├── domain
│   └── presentation
└── payment
    ├── application
    ├── domain
    ├── infrastructure
    └── presentation
```

## 실행 방법

### 1. 환경 변수 설정

`.env.example`을 참고해 프로젝트 루트에 `.env` 파일을 만들고 Toss Payments 테스트 키를 설정합니다.

```text
TOSS_SECRET_KEY=your_toss_secret_key
TOSS_CLIENT_KEY=your_toss_client_key
```

이 프로젝트는 `application.yaml`에서 루트의 `.env` 파일을 선택적으로 읽도록 설정되어 있습니다. `.env`는 Git에 올라가지 않도록 `.gitignore`에 등록되어 있습니다.

Windows PowerShell에서 환경 변수로 직접 주입하려면 다음처럼 설정할 수도 있습니다.

```powershell
$env:TOSS_SECRET_KEY="your_toss_secret_key"
$env:TOSS_CLIENT_KEY="your_toss_client_key"
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

Windows에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat bootRun
```

기본 프로필은 `dev`이며, H2 인메모리 DB를 사용합니다.

### 3. H2 콘솔 접속

```text
http://localhost:8080/h2-console
```

접속 정보:

```text
JDBC URL: jdbc:h2:mem:tosspayments
Username: sa
Password:
```

## 주요 API

### 주문 생성

결제 버튼을 누르기 전 서버에 주문 금액을 저장합니다. 이후 결제 승인 단계에서 클라이언트가 전달한 금액과 서버에 저장된 금액을 비교합니다.

```http
POST /orders/prepare
Content-Type: application/json
```

```json
{
  "orderId": "order-20260414-001",
  "orderName": "테스트 상품",
  "amount": 10000,
  "customerEmail": "test@example.com",
  "customerName": "홍길동"
}
```

### 주문 조회

```http
GET /orders/{orderId}
```

### 결제 승인

Toss Payments 결제창에서 성공 리다이렉트 후 받은 `paymentKey`, `orderId`, `amount`로 결제를 승인합니다.

```http
POST /payments/confirm
Content-Type: application/json
```

```json
{
  "paymentKey": "payment-key-from-toss",
  "orderId": "order-20260414-001",
  "amount": 10000
}
```

### 결제 취소

`cancelAmount`를 생략하면 전액 취소, 값을 전달하면 부분 취소로 동작합니다.

```http
POST /payments/{paymentKey}/cancel
Content-Type: application/json
```

```json
{
  "cancelReason": "고객 요청",
  "cancelAmount": 5000
}
```

### 결제 조회

```http
GET /payments/{paymentKey}
GET /payments/order/{orderId}
GET /payments
```

### 웹훅 수신

Toss Payments 개발자센터 웹훅 URL에 아래 엔드포인트를 등록합니다.

```http
POST /webhooks/toss
```

현재 구현은 `orderId`, `status`, `secret` 값을 받아 저장된 결제 및 주문 상태를 갱신합니다.

### 빌링키 발급

```http
POST /billing/authorize
Content-Type: application/json
```

```json
{
  "authKey": "auth-key-from-toss",
  "customerKey": "customer-001"
}
```

### 자동결제 실행

```http
POST /billing/{billingKey}/charge
Content-Type: application/json
```

```json
{
  "customerKey": "customer-001",
  "amount": 10000,
  "orderId": "billing-order-001",
  "orderName": "정기결제 상품",
  "customerEmail": "test@example.com",
  "customerName": "홍길동"
}
```

## 테스트

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```

## 프로필 설정

### dev

- H2 인메모리 DB 사용
- `ddl-auto: create-drop`
- SQL 로그 출력
- H2 콘솔 활성화

### prod

- MySQL 사용
- `DB_USERNAME`, `DB_PASSWORD` 환경 변수 필요
- `ddl-auto: validate`

```yaml
spring:
  profiles:
    active: prod
```

## 결제 흐름

1. 클라이언트가 `POST /orders/prepare`로 주문 정보를 서버에 저장합니다.
2. 클라이언트가 Toss Payments 결제창을 호출합니다.
3. 결제 성공 후 받은 `paymentKey`, `orderId`, `amount`를 `POST /payments/confirm`으로 전달합니다.
4. 서버는 저장된 주문 금액과 요청 금액을 검증합니다.
5. 서버가 Toss Payments 결제 승인 API를 호출합니다.
6. 승인 성공 시 결제 정보를 저장하고 주문 상태를 결제 완료로 변경합니다.

## 참고

이 프로젝트는 Toss Payments 연동 학습을 위한 토이 프로젝트입니다.  
실서비스에 적용하려면 인증/인가, 웹훅 서명 검증, 멱등성 처리, 운영 로그, 장애 대응, 민감 정보 관리, 주문 재고 처리 등을 추가로 고려해야 합니다.
