# 설계 노트

## 위치: 애플리케이션은 위경도만, 공간 연산은 DB 가

`products.location` 은 `GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED`.
JPA 엔티티는 `latitude`, `longitude` 두 double 만 갖고 `location` 은 매핑하지 않는다.

- hibernate-spatial / JTS 의존 없이 끝난다.
- 반경 검색은 네이티브 SQL 하나 (`ProductSearchRepositoryImpl`). 파라미터는 전부 바인딩.
- `geography` 를 쓴 이유: 미터 단위 거리가 바로 나온다. `geometry` 는 도(degree) 단위라 위도에 따라 왜곡.

상품의 위치는 **등록 시점 판매자 위치의 스냅샷**이다. 판매자가 이사해도 이미 올린 글의 거래 장소는 그대로여야 한다.

## 목록: 키셋 커서

정렬 키 `(bumped_at DESC, id DESC)`. 커서는 마지막 항목의 `bumped_at|id` 를 base64url 로 감싼 불투명 문자열.
`끌어올리기` 는 `bumped_at` 만 갱신하므로 정렬 맨 위로 올라가고, 커서 페이징은 흔들리지 않는다(이미 지나간 페이지에 다시 나타나지 않음).

`hasNext` 는 `size + 1` 건을 조회해 판단한다. COUNT 쿼리를 없애기 위해서.

## 인증: Spring Security 의 JWT 도구만으로

`spring-security-oauth2-jose` 의 `NimbusJwtEncoder`/`NimbusJwtDecoder` 에 HMAC 대칭키. 외부 JWT 라이브러리 없음.
`@CurrentMember` 는 `@AuthenticationPrincipal` 의 SpEL 로 subject 를 Long 으로 바꾼다. 비로그인이면 `null` — 목록·상세는 비로그인도 볼 수 있어야 하므로.

리프레시 토큰은 의도적으로 뺐다(7일 액세스 토큰). 포트폴리오 범위에서 리프레시 로테이션·탈취 감지까지 가면 인증이 주제가 돼버린다.

## 채팅

```
클라이언트 ── STOMP SEND /app/chat/{roomId} ──▶ ChatStompController
                                                    │ ChatService.send (트랜잭션)
                                                    │   저장 → room.touch → afterCommit
                                                    ▼
                                    /topic/rooms/{roomId}  ← 방 참여자 (메시지 / 읽음 이벤트)
                                    /topic/users/{상대}    ← 방 목록·배지 갱신 신호
```

- **인증**: HTTP 필터는 WebSocket 프레임을 못 본다. `StompAuthInterceptor` 가 CONNECT 프레임의 `Authorization` 을 검증해 세션 Principal 로 등록한다.
- **인가**: SUBSCRIBE 시 방 참여자인지 검사. 아니면 예외 → 서버가 ERROR 프레임을 보내고 연결을 끊는다 (`ChatStompTest` 로 검증).
- **REST 폴백**: WebSocket 이 끊긴 순간에도 메시지는 보내져야 한다. `POST /messages` 가 같은 서비스 메서드를 호출하므로 브로드캐스트도 동일하게 나간다.
- **읽음**: `read_at` 컬럼. 상대가 방에 들어오거나 새 메시지를 받으면 `markRead` → READ 이벤트로 내 화면의 "1" 이 사라진다.
- **단일 인스턴스 브로커**: `enableSimpleBroker`. 인스턴스가 늘면 `enableStompBrokerRelay` 로 RabbitMQ/Redis 를 붙이는 지점 — 코드는 그대로.

## 찜: 원자성

토글은 (1) 존재 확인 (2) INSERT/DELETE (3) `like_count ± 1`. 두 요청이 동시에 오면 (1)을 둘 다 통과할 수 있다.
- INSERT 는 `UNIQUE(member_id, product_id)` 가 두 번째를 막는다 → `DataIntegrityViolationException` 을 잡아 "이미 찜" 으로 응답.
- 카운트는 엔티티에서 읽어 +1 하지 않고 `UPDATE ... SET like_count = like_count + 1` 로 DB 가 계산한다.

## 캐시 무효화 범위

`productDetail` 캐시를 지우는 곳: 수정, 상태 변경, 끌어올리기, 삭제, 찜 토글. 조회수는 캐시를 건드리지 않는다(Redis 에서 더함).
"캐시에 뭘 넣을까" 보다 "언제 지울까" 가 어렵다 — 찜 토글이 상세 캐시를 지운다는 걸 `LikeService` 가 알아야 하는 결합이 생겼다. 이벤트로 풀 수 있지만 지금 규모에선 과하다고 판단.

## 프론트: WebSocket 연결 하나

`StompProvider` 가 로그인 상태에서 연결 1개를 만들고 모든 페이지가 공유한다. 채팅방 페이지는 방 토픽만 구독/해제.
`useUnread` 는 개인 토픽을 구독해 하단 탭 배지를 갱신한다. 끊기면 stompjs 가 5초 간격으로 재접속하고, 재접속 사이엔 REST 폴백으로 전송된다.

## 확장 지점

- 이미지: 로컬 디스크 → S3 presigned URL (서버 무경유 업로드)
- 동네: 프리셋 10개 → 행정동 DB + 역지오코딩
- 검색: trigram → Elasticsearch / pgvector 의미 검색
- 알림: 채팅 도착 시 푸시(FCM), 오프라인 사용자
- 브로커: 심플 브로커 → 외부 브로커 릴레이 (다중 인스턴스)
