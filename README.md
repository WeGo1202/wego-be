# 스마트 관광 경로(Enjoy Trip) – 백엔드 & 프론트엔드 통합 README

> 이 문서는 **현재 소스 코드(src + frontsrc)** 기준으로 정리한 최신 README입니다.  
> 기존 `Member / Article` 샘플 구조가 아니라, **JWT 로그인 + Plan + Route** 중심 구조를 기준으로 작성되었습니다.

---

## 1. 프로젝트 개요

### 1.1 서비스 목적

- 한국관광공사 **TourAPI**를 통해 관광지 정보를 수집하고,
- **Kakao 지도** 위에 시각화하여
- 사용자가 **나만의 여행 플랜(Plan)** 을 만들고, 여러 플랜을 묶어 **여행 루트(Route)** 를 구성할 수 있는 스마트 관광 서비스.

### 1.2 주요 기능

1. **회원 / 인증**
   - 이메일 기반 **회원가입** (`/api/auth/signup`)
   - **로그인 후 JWT 발급** (`/api/auth/login`)
   - 프론트에서 토큰을 `localStorage` 에 저장하고, Axios 인터셉터로 `Authorization: Bearer` 자동 전송

2. **관광지 조회 (공공데이터 연동)**
   - 백엔드에서 **TourAPI** 호출 → 관광지 정보 취합
   - `/api/map/attractions` REST API로 프론트에 제공
   - 프론트(Vue)에서 Kakao 지도에 마커로 표시

3. **여행 플랜(Plan) 관리**
   - 지도에서 선택한 장소를 기반으로 **Plan 생성** (`POST /api/plans`)
   - 로그인한 사용자의 **내 플랜 목록 조회** (`GET /api/plans/me`)

4. **여행 루트(Route) 관리**
   - 사용자가 **여행 루트(Route) 생성** (`POST /api/routes`)
   - 내 루트 목록 조회 (`GET /api/routes/me`)
   - 루트 상세 조회 (`GET /api/routes/{id}`)
   - 루트 삭제 (`DELETE /api/routes/{id}`)
   - 특정 루트에 Plan 추가 (`POST /api/routes/{routeId}/plans`)

5. **SPA 기반 화면 구성**
   - Vue 3 + Vue Router 기반 단일 페이지 앱
   - 메인(Home), 로그인/회원가입, 지도(Map), 내 플랜(My Plans) 화면 제공
   - 라우터 가드로 **인증이 필요한 페이지 접근 제어**

---

## 2. 기술 스택 및 환경

### 2.1 Backend

- **Language**: Java 17  
- **Framework**: Spring Boot (Web, Security, Data JPA)  
- **DBMS**: MySQL 8.x (`ssafytrip` 스키마 사용)  
- **ORM**: Spring Data JPA (Hibernate)  
- **Security**: Spring Security + JWT (io.jsonwebtoken)  
- **Session**: `spring-session-jdbc` 설정 포함 (JDBC 기반 세션 테이블 사용 가능)  
- **Build**: Gradle  
- **Lombok**: `@Data`, `@Builder`, `@RequiredArgsConstructor` 등 사용  

**외부 API**

- 한국관광공사 **TourAPI** – 관광지 리스트 조회
- **Kakao Maps JavaScript API** – 지도 및 마커 표시

### 2.2 Frontend

- **Vue 3** (Composition API)  
- **Vite** 빌드 시스템  
- **Vue Router** – SPA 라우팅  
- **Axios** – 백엔드 REST API 호출  
- **Bootstrap 5**  
- **Kakao Maps JS** – 지도, 마커, 클러스터링 (`libraries=clusterer`)

### 2.3 주요 설정

#### Backend – `application.properties` (예시)

```properties
spring.application.name=demo

# DB
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3308/ssafytrip?serverTimezone=Asia/Seoul&useUniCode=yescharacterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=ssafy

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect

spring.session.jdbc.initialize-schema=always

# Kakao 지도
kakao.maps.key=카카오_지도_JAVASCRIPT_키

# JWT
jwt.issuer=tour-app
jwt.secret-key=THIS_IS_A_VERY_SECRET_JWT_KEY_123456
jwt.access-token-expire-ms=3600000
```

#### Frontend – `.env` (예시)

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_KAKAO_JS_KEY=카카오_지도_JAVASCRIPT_키
```

---

## 3. 전체 아키텍처

### 3.1 백엔드 패키지 구조

```text
com.ssafy.trip
 ├─ DemoApplication                 # Spring Boot main
 ├─ config
 │   ├─ JwtProperties               # jwt.* 설정 바인딩 (issuer, secret, expire)
 │   ├─ TokenProvider               # JWT 생성/검증, Authentication 생성
 │   ├─ TokenAuthenticationFilter   # 요청 헤더의 Bearer 토큰 파싱 → SecurityContext 저장
 │   └─ WebSecurityConfig           # Spring Security 설정 (JWT 필터, CORS, 권한)
 ├─ controller
 │   ├─ AuthController              # /api/auth (signup, login)
 │   ├─ MapController               # /api/map/attractions – TourAPI → 관광지 목록
 │   ├─ PlanController              # /api/plans – Plan 생성/조회
 │   └─ RouteController             # /api/routes – Route 생성/조회/삭제/플랜추가
 ├─ domain
 │   ├─ Member                      # 사용자 계정
 │   ├─ Plan                        # 단일 여행 계획(장소 + 정보)
 │   ├─ Route                       # 여러 Plan을 묶은 루트
 │   ├─ RoutePlan                   # Route : Plan 매핑 + 일자/순서 정보
 │   └─ Address                     # Embeddable 주소/좌표 정보
 ├─ dto
 │   ├─ SignupRequest               # 회원가입 요청
 │   ├─ PlanRequest                 # Plan 생성 시 사용하는 DTO
 │   └─ RouteCreateRequest          # Route 생성 시 사용하는 DTO
 ├─ repository
 │   ├─ MemberRepository            # findByEmail, existsByEmail
 │   ├─ PlanRepository              # findByMember
 │   └─ RouteRepository             # findByMember
 └─ service
     ├─ AuthService                 # signup, login, JWT 발급
     ├─ MapService                  # TourAPI 호출 → attractions 데이터 가공
     ├─ PlanService                 # Plan 생성/조회
     └─ RouteService                # Route 생성/조회/삭제/플랜추가
```

### 3.2 프론트엔드 디렉터리 구조

```text
frontsrc
 └─ src
     ├─ main.js               # Vue 앱 진입점
     ├─ App.vue               # 전체 레이아웃, 상단 네비게이션
     ├─ api
     │   └─ axios.js          # Axios 인스턴스 (baseURL + JWT 인터셉터)
     ├─ router
     │   └─ index.js          # Vue Router 설정, beforeEach 가드
     ├─ stores
     │   └─ attractionStore.js# 관광지 캐시 스토어 (Composition API)
     ├─ utils
     │   ├─ auth.js           # localStorage 토큰 관리 + authState
     │   └─ kakao.js          # Kakao Maps JS 동적 로딩
     └─ views
         ├─ HomeView.vue      # 메인 랜딩/Hero 화면
         ├─ LoginView.vue     # 로그인 폼
         ├─ SignupView.vue    # 회원가입 폼
         ├─ MapView.vue       # 관광지 지도 + 플랜/루트 관리 UI
         └─ MyPlansView.vue   # 내 플랜 목록
```

---

## 4. 도메인 모델

### 4.1 Member

```java
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;      // 로그인 ID

    private String password;   // BCrypt 암호화
    private String name;
    private String nickname;
    private String phone;
    private String address;
    private String role;       // "ROLE_USER" 등

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Plan> plans = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Route> routes = new ArrayList<>();
}
```

### 4.2 Address (Embeddable)

```java
@Embeddable
@Data
@NoArgsConstructor
public class Address {
    private String name;      // 장소명(필요 시 사용)
    private String sido;      // 시도
    private String gugun;     // 구/군
    private String town;      // 읍/면/동

    @Column(name = "latitude", columnDefinition = "FLOAT")
    private Float latitude;   // 위도
    @Column(name = "longitude", columnDefinition = "FLOAT")
    private Float longitude;  // 경도

    @Column(name="detail_Address")
    private String detailAddress; // 상세 주소
}
```

### 4.3 Plan

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "plan")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Plan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String thumbnail;
    private Integer period;

    @Column(name="is_public")
    @ColumnDefault("false")
    private boolean isPublic;

    @Embedded
    private Address location;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
```

### 4.4 Route & RoutePlan

```java
@Entity
@Table(name = "route")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Integer totalDays;      // 총 일수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore
    private Member member;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayIndex ASC, orderIndex ASC")
    private List<RoutePlan> routePlans = new ArrayList<>();
}
```

```java
@Entity
@Table(name = "route_plan")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoutePlan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer dayIndex;   // 1일차, 2일차 ...
    private Integer orderIndex; // 해당 일자의 순서

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonIgnore
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
}
```

---

## 5. 보안 / 인증 구조

### 5.1 JwtProperties & TokenProvider

- `JwtProperties`  
  - `jwt.issuer`, `jwt.secret-key`, `jwt.access-token-expire-ms` 를 바인딩하는 `@ConfigurationProperties(prefix = "jwt")` 레코드.
- `TokenProvider`  
  - 로그인 시 **JWT 발급**
  - HTTP 요청에서 들어온 JWT를 파싱해 `Claims` 확인
  - 유효한 토큰인 경우 `UsernamePasswordAuthenticationToken` 생성하여 `Authentication` 제공

### 5.2 TokenAuthenticationFilter

- 모든 요청에 대해 **`Authorization: Bearer <token>`** 헤더를 검사
- 토큰이 유효하면 `SecurityContextHolder`에 Authentication을 세팅
- 이후 컨트롤러에서 `Authentication authentication` 파라미터로 현재 로그인 사용자의 이메일을 가져옴 (`authentication.getName()`)

### 5.3 WebSecurityConfig (요약)

- CSRF 비활성화, 세션 **STATELESS**
- CORS 설정
- 다음 경로는 **인증 없이 허용**:
  - `/`, `/index`, `/login`, `/signup`
  - 정적 리소스: `/css/**`, `/js/**`, `/images/**`
  - 인증 API: `/api/auth/**`
  - 지도/공공데이터 조회: `/api/map/**`
- 그 외 `/api/plans/**`, `/api/routes/**` 등 대부분의 API는 **인증 필요**
- `TokenAuthenticationFilter` 를 `UsernamePasswordAuthenticationFilter` **앞에** 추가

---

## 6. REST API 상세

### 6.1 인증 API – `/api/auth`

#### 6.1.1 회원가입 – `POST /api/auth/signup`

**Request Body (JSON)** – `SignupRequest`

```json
{
  "email": "user@test.com",
  "password": "1234",
  "name": "홍길동",
  "nickname": "길동이",
  "phone": "010-0000-0000"
}
```

**응답**

- 성공: `200 OK`
- 이메일 중복: `409 CONFLICT`

#### 6.1.2 로그인 – `POST /api/auth/login`

**Request Body (JSON)**

```json
{
  "email": "user@test.com",
  "password": "1234"
}
```

**성공 Response 예시**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQHRlc3QuY29tIiwiaXNzIjoidG91ci1hcHAiLCJleHAiOjE3MzI1NjE2MDB9.dummy-signature"
}
```

---

### 6.2 지도 / 관광지 API – `/api/map`

#### 6.2.1 관광지 목록 조회 – `GET /api/map/attractions`

**Response 예시**

```json
[
  {
    "title": "경복궁",
    "address": "서울특별시 종로구 사직로 161",
    "lat": 37.579617,
    "lng": 126.977041
  },
  {
    "title": "남산서울타워",
    "address": "서울특별시 용산구 남산공원길 105",
    "lat": 37.551169,
    "lng": 126.988227
  }
]
```

---

### 6.3 Plan API – `/api/plans`

> 모든 Plan 관련 API는 JWT 인증 필요 (`Authorization: Bearer <token>`)

#### 6.3.1 Plan 생성 – `POST /api/plans`

**Request Body (PlanRequest)**

```json
{
  "title": "경복궁 야간관람",
  "description": "야경이 예쁜 경복궁 투어",
  "thumbnail": "https://example.com/img/kyungbokgung.png",
  "period": 1,
  "public": true,
  "name": "경복궁",
  "sido": "서울특별시",
  "gugun": "종로구",
  "town": "세종로",
  "latitude": 37.579617,
  "longitude": 126.977041,
  "detailAddress": "사직로 161"
}
```

**Response 예시**

```json
{
  "id": 1,
  "title": "경복궁 야간관람",
  "description": "야경이 예쁜 경복궁 투어",
  "thumbnail": "https://example.com/img/kyungbokgung.png",
  "period": 1,
  "public": true,
  "location": {
    "name": "경복궁",
    "sido": "서울특별시",
    "gugun": "종로구",
    "town": "세종로",
    "latitude": 37.579617,
    "longitude": 126.977041,
    "detailAddress": "사직로 161"
  },
  "member": {
    "id": 2,
    "email": "user@test.com",
    "nickname": "길동이"
  }
}
```

#### 6.3.2 내 플랜 목록 조회 – `GET /api/plans/me`

**Response 예시**

```json
[
  {
    "id": 1,
    "title": "경복궁 야간관람",
    "period": 1,
    "public": true
  },
  {
    "id": 2,
    "title": "남산타워 전망대",
    "period": 1,
    "public": false
  }
]
```

---

### 6.4 Route API – `/api/routes`

#### 6.4.1 Route 생성 – `POST /api/routes`

**Request Body (RouteCreateRequest)**

```json
{
  "title": "서울 2박 3일 코스",
  "description": "경복궁, 남산타워, 한강 포함",
  "totalDays": 3,
  "items": [
    { "planId": 1, "dayIndex": 1, "orderIndex": 1 },
    { "planId": 2, "dayIndex": 1, "orderIndex": 2 }
  ]
}
```

**Response 예시**

```json
{
  "id": 10,
  "title": "서울 2박 3일 코스",
  "description": "경복궁, 남산타워, 한강 포함",
  "totalDays": 3,
  "routePlans": [
    {
      "id": 100,
      "dayIndex": 1,
      "orderIndex": 1,
      "plan": { "id": 1, "title": "경복궁 야간관람" }
    },
    {
      "id": 101,
      "dayIndex": 1,
      "orderIndex": 2,
      "plan": { "id": 2, "title": "남산타워 전망대" }
    }
  ]
}
```

#### 6.4.2 내 루트 목록 조회 – `GET /api/routes/me`

```json
[
  {
    "id": 10,
    "title": "서울 2박 3일 코스",
    "totalDays": 3
  },
  {
    "id": 11,
    "title": "부산 1박 2일 코스",
    "totalDays": 2
  }
]
```

#### 6.4.3 루트 상세 조회 – `GET /api/routes/{id}`

```json
{
  "id": 10,
  "title": "서울 2박 3일 코스",
  "description": "경복궁, 남산타워, 한강 포함",
  "totalDays": 3,
  "routePlans": [
    {
      "dayIndex": 1,
      "orderIndex": 1,
      "plan": {
        "id": 1,
        "title": "경복궁 야간관람"
      }
    }
  ]
}
```

#### 6.4.4 루트 삭제 – `DELETE /api/routes/{id}`

- 성공: `204 NO CONTENT` 또는 `200 OK`
- 권한 없음: `403 FORBIDDEN`

#### 6.4.5 루트에 Plan 추가 – `POST /api/routes/{routeId}/plans`

**Request Body**

- `PlanRequest`와 동일 구조

**Response**

- 업데이트된 Route 정보 반환 (위 6.4.1 Response와 유사)

---

## 7. Postman 컬렉션 예시 JSON

```json
{
  "info": {
    "name": "EnjoyTrip API Collection",
    "_postman_id": "11111111-2222-3333-4444-555555555555",
    "description": "스마트 관광 경로(EnjoyTrip) 백엔드 API 테스트용 Postman 컬렉션 예시",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth - Signup",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"user@test.com\",\n  \"password\": \"1234\",\n  \"name\": \"홍길동\",\n  \"nickname\": \"길동이\",\n  \"phone\": \"010-0000-0000\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/auth/signup",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "auth", "signup"]
        }
      }
    },
    {
      "name": "Auth - Login",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"user@test.com\",\n  \"password\": \"1234\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/auth/login",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "auth", "login"]
        }
      }
    },
    {
      "name": "Map - Attractions",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/map/attractions",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "map", "attractions"]
        }
      }
    },
    {
      "name": "Plan - Create",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Authorization", "value": "Bearer {{accessToken}}" }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"title\": \"경복궁 야간관람\",\n  \"description\": \"야경이 예쁜 경복궁 투어\",\n  \"thumbnail\": \"https://example.com/img/kyungbokgung.png\",\n  \"period\": 1,\n  \"public\": true,\n  \"name\": \"경복궁\",\n  \"sido\": \"서울특별시\",\n  \"gugun\": \"종로구\",\n  \"town\": \"세종로\",\n  \"latitude\": 37.579617,\n  \"longitude\": 126.977041,\n  \"detailAddress\": \"사직로 161\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/plans",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "plans"]
        }
      }
    },
    {
      "name": "Route - Create",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" },
          { "key": "Authorization", "value": "Bearer {{accessToken}}" }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"title\": \"서울 2박 3일 코스\",\n  \"description\": \"경복궁, 남산타워, 한강 포함\",\n  \"totalDays\": 3,\n  \"items\": [\n    { \"planId\": 1, \"dayIndex\": 1, \"orderIndex\": 1 },\n    { \"planId\": 2, \"dayIndex\": 1, \"orderIndex\": 2 }\n  ]\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/routes",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "routes"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "accessToken",
      "value": ""
    }
  ]
}
```




## 8. 실행 방법 요약

### 8.1 Backend 실행

1. **MySQL 설정**
   - 데이터베이스 생성:
     ```sql
     CREATE DATABASE ssafytrip CHARACTER SET utf8mb4;
     ```
   - `application.properties` 의 DB URL/계정/비밀번호를 환경에 맞게 수정.

2. **JWT & Kakao Key 설정**
   - `jwt.secret-key` 를 충분히 긴 랜덤 문자열로 변경.
   - `kakao.maps.key` 에 발급받은 Kakao JavaScript 키 입력.

3. **빌드 & 실행**

   ```bash
   ./gradlew bootRun
   ```

   또는 IDE에서 `DemoApplication` 실행.

4. **확인**
   - `http://localhost:8080` 접속 혹은 Postman으로 `/api/map/attractions` 테스트.

### 8.2 Frontend 실행

1. **의존성 설치**

   ```bash
   cd frontsrc
   npm install
   ```

2. **환경 변수 설정**

   `.env` 파일 생성:

   ```env
   VITE_API_BASE_URL=http://localhost:8080
   VITE_KAKAO_JS_KEY=카카오_지도_JAVASCRIPT_키
   ```

3. **개발 서버 실행**

   ```bash
   npm run dev
   ```

4. **접속**

   - `http://localhost:5173` 접속
   - 회원가입 → 로그인 → `/map`에서 지도 + 플랜/루트 기능 확인

---

## 9. 향후 확장 아이디어

- Plan/Route에 **태그, 메모, 일정 시간, 인원 수** 등의 메타데이터 추가
- 다른 공공데이터(Tmap, 공영주차장, 공공화장실, 날씨 API 등)와 연동해 지도 오버레이 확장
- **추천 알고리즘**(인기 루트, 유사 사용자 추천 코스 등) 탑재
- **댓글/좋아요** 기능을 통해 나만의 루트를 공유하고 피드백 받을 수 있는 커뮤니티 기능
- Swagger/OpenAPI를 적용하여 API 문서 자동화 및 테스트 UI 제공
