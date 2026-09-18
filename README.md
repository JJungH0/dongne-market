# 동네마켓 — 위치 기반 중고 직거래 (당근마켓 클론)

내 동네 반경 안의 중고 물건을 찾고, 판매자와 실시간으로 채팅하는 풀스택 서비스입니다.
클론이지만 **"흔한 CRUD"가 아니라 실제 서비스가 겪는 문제 4가지**에 집중했습니다.

| 문제 | 해결 | 실측 |
|---|---|---|
| 위치 기반 검색 | PostGIS `geography` 생성 컬럼 + GIST 인덱스, `ST_DWithin` | Seq Scan 37.7ms → **9.0ms** |
| 무한 스크롤 목록 | 키셋 커서 `(bumped_at, id) < (?, ?)` + 복합 인덱스 | OFFSET 3.8ms → **0.38ms**, 중복/누락 없음 |
| 제목 검색 | pg_trgm GIN | 4.5ms → **1.0ms** |
| 인기 상품 조회수 | Redis INCR write-behind + 중복 조회 제거 | 상세 조회당 DB UPDATE 0회 |
| 실시간 채팅 | WebSocket/STOMP, JWT 프레임 인증, 구독 권한 검사, 읽음 처리 | 커밋 후 브로드캐스트 |

수치와 플랜은 [docs/performance.md](docs/performance.md), 설계 결정은 [docs/architecture.md](docs/architecture.md).

```
Backend   Java 21 · Spring Boot 4.1 · Spring Security 7 (JWT/HMAC) · Spring Data JPA · WebSocket/STOMP
          PostgreSQL 17 + PostGIS + pg_trgm · Redis 7 · Flyway · Testcontainers · springdoc
Frontend  React 19 · TypeScript · Vite · React Router · @stomp/stompjs (빌드 결과를 Spring 이 서빙)
```

## 화면

모바일 앱형 중앙 컬럼(데스크톱에선 2열 그리드), 다크 모드 자동.

- **홈**: 내 동네·반경 기준 목록, 거리 표시, 카테고리 칩, 검색, 무한 스크롤, 글쓰기 FAB
- **상품 상세**: 이미지 캐러셀, 판매자 매너온도, 관심(찜), 채팅하기 / 판매자에겐 상태 변경·끌어올리기·수정·삭제
- **글쓰기**: 이미지 업로드(최대 10장, 대표 이미지), 카테고리, 가격(0원 = 나눔)
- **채팅**: 방 목록(안 읽은 수, 실시간 갱신), 채팅방(실시간 수신, 읽음 표시 "1", 이전 메시지 커서), 연결 상태 표시
- **내 동네 설정**: 동네 프리셋 또는 현재 위치(Geolocation), 반경 1~20km 슬라이더
- **나의 동네**: 판매내역, 관심목록

## 빠른 시작

```bash
docker compose up -d      # PostGIS(5435) + Redis(6382)
./gradlew bootRun         # 프론트 빌드(npm) 포함. Node 20+ 필요
```

- 앱: http://localhost:8080 · Swagger: http://localhost:8080/swagger-ui.html
- 시드 계정: `minsu@example.com` / `yuna@` / `jihoon@` / `sora@` — 비밀번호 `password123`
- 기본 프로파일은 성능 측정용 **2만 건** 시드가 함께 들어갑니다 (`db/seed-bulk`).

Apple Silicon 은 공식 `postgis/postgis` 이미지에 arm64 가 없어 `imresamu/postgis` 를 씁니다 (compose·테스트 모두).

프론트 개발 서버: `cd frontend && npm run dev` (http://localhost:5173, `/api`·`/ws` 프록시)

## API 요약

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/auth/signup`, `/api/auth/login` | JWT 발급 |
| GET/PUT | `/api/me`, `/api/me/region` | 내 정보, 동네·반경 변경 |
| GET | `/api/products?lat&lng&radiusKm&category&keyword&cursor&size` | 목록 (로그인 시 위치 생략 → 내 동네) |
| GET | `/api/products/{id}` | 상세 (조회수 집계, liked/mine) |
| POST/PUT/DELETE | `/api/products[/{id}]` | 등록/수정/삭제 (본인만) |
| PATCH | `/api/products/{id}/status` | 판매중/예약중/거래완료 |
| POST | `/api/products/{id}/bump`, `/like` | 끌어올리기(하루 1회), 찜 토글 |
| POST | `/api/images` | 이미지 업로드 (multipart, 5MB) |
| POST/GET | `/api/chat/rooms` | 방 열기(멱등) / 내 방 목록 |
| GET/POST | `/api/chat/rooms/{id}/messages` | 메시지 커서 조회 / 전송(REST 폴백) |
| POST | `/api/chat/rooms/{id}/read` | 읽음 처리 |
| WS | `/ws` → SEND `/app/chat/{roomId}`, SUBSCRIBE `/topic/rooms/{roomId}`, `/topic/users/{me}` | 실시간 |

## 테스트

```bash
./gradlew test     # Docker 필요 (Testcontainers: PostGIS + Redis)
```

| 테스트 | 검증 |
|---|---|
| `AuthApiTest` (5) | 가입→토큰→내 정보, 중복 409, 잘못된 비밀번호 401, 공개/보호 API, 동네 변경 시 목록 기준점 변경 |
| `ProductApiTest` (7) | 반경 3km 필터·거리, **커서로 끝까지 순회해 중복/누락 0**, 잘못된 커서 400, 키워드/카테고리, 본인만 수정·삭제, 끌어올리기 하루 1회, 찜 토글·카운트 |
| `ViewCounterTest` (1) | 같은 사용자 중복 조회 제거, flush 후 DB 합산 |
| `CursorTest` (2) | 커서 인코딩 왕복 |
| `ChatApiTest` (3) | 방 멱등, 안 읽은 수, 읽음 처리, 메시지 커서, 비참여자 403 |
| `ChatStompTest` (3) | **실제 WebSocket**: JWT CONNECT → 구독 → 전송 → 상대 수신, 토큰 없는 CONNECT 거부, 남의 방 구독 시 ERROR |

## 프로젝트 구조

```
com.dongne
├── security     JwtConfig(HMAC Encoder/Decoder), TokenService, SecurityConfig, @CurrentMember
├── member       회원, 동네·반경, 인증 API
├── region       동네 프리셋
├── product      Product, 네이티브 검색(PostGIS+커서), ViewCounter(Redis), 캐시
├── like         찜 (UNIQUE + 원자적 카운트)
├── image        로컬 업로드
├── chat         ChatRoom/Message, ChatService(커밋 후 브로드캐스트), STOMP 컨트롤러, StompAuthInterceptor
├── config       WebSocket, Cache, 정적 리소스, SPA 포워딩
└── common       예외 처리(ProblemDetail), CursorPage

frontend/src
├── pages        Home, ProductDetail, Write, ChatList, ChatRoom, Login/Signup, Region, Me
├── hooks        useAuth, useStomp(연결 1개 공유·자동 재접속), useUnread, useToast
└── lib          api(토큰 주입·401 처리), types, format
```
