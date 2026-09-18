# 성능 노트 — 실측

환경: PostgreSQL 17 + PostGIS 3.5 (Docker, Apple M-series), `products` 20,010건 (`V100__bulk_products.sql`), `scripts/explain-benchmark.sh` 로 측정.
수치는 `EXPLAIN (ANALYZE, BUFFERS)` 의 Execution Time. 작은 데이터라 절대값보다 **플랜의 변화**가 핵심이다.

## 1. 반경 검색 — GIST 인덱스

```sql
WHERE ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, 3000)
```

| | 플랜 | 실행 시간 |
|---|---|---|
| GIST 없음 | Seq Scan 20,010행 전부에 거리 계산 | **37.7 ms** |
| GIST 있음 | Bitmap Index Scan → 후보 858행만 정밀 검사 | **9.0 ms** |

- `geography` 컬럼은 위경도로부터 DB 가 계산하는 **생성 컬럼**이라 애플리케이션은 공간 타입을 몰라도 된다.
- 20만 건, 200만 건으로 가면 Seq Scan 은 선형으로 늘지만 GIST 는 후보 수(반경 안 밀도)에만 비례한다.

## 2. 제목 부분 일치 — pg_trgm GIN

`LIKE '%키워드%'` 는 앞에 `%` 가 붙는 순간 B-tree 를 못 탄다. trigram 인덱스는 3글자 조각으로 후보를 좁힌다.

| | 플랜 | 실행 시간 |
|---|---|---|
| 인덱스 없음 | Seq Scan (2만 행 문자열 비교) | **4.5 ms** |
| GIN (gin_trgm_ops) | Bitmap Index Scan → 1,000 후보 | **1.0 ms** |

한글도 trigram 이 동작한다(문자 단위). 다만 2글자 이하 검색어는 trigram 이 안 만들어져 인덱스를 못 탄다 — 서비스에서는 최소 2글자 + 인덱스 미적용 허용으로 타협.

## 3. 페이징 — OFFSET vs 키셋 커서

900페이지째(18,000건 뒤)를 가져올 때:

| | 플랜 | 실행 시간 |
|---|---|---|
| `OFFSET 18000 LIMIT 21` | 18,009행을 읽고 **버림** | **3.8 ms** |
| 키셋 `(bumped_at, id) < (?, ?)` + 복합 인덱스 없음 | Seq Scan | 1.4 ms |
| 키셋 + `(status, bumped_at DESC, id DESC)` | **Index Only Scan**, 9행만 읽음 | **0.38 ms** |

OFFSET 은 페이지가 뒤로 갈수록 읽고 버리는 양이 커진다(O(offset)). 키셋은 어느 페이지든 O(page size). 그리고 OFFSET 은 그 사이 새 글이 끼어들면 **중복/누락**이 생기는데, 무한 스크롤에서 이게 더 치명적이다 — 그래서 API 가 커서를 불투명 문자열로 내려준다.

행 비교 `(a, b) < (x, y)` 는 Postgres 가 `a < x OR (a = x AND b < y)` 로 풀지 않고 복합 인덱스에 그대로 태운다.

## 4. 조회수 — Redis write-behind

상세 조회마다 `UPDATE products SET view_count = view_count + 1` 을 치면 인기 상품 한 행에 UPDATE 가 몰려 행 락 대기 + WAL 폭증.
`INCR product:view:{id}` 로 받아두고 30초마다 한 번에 반영한다(`ViewCounter.flush`). 같은 사용자의 1시간 내 재조회는 `SET NX EX` 로 거른다.

트레이드오프: Redis 가 죽으면 최대 30초분 조회수가 유실된다. 조회수는 정확성보다 부하 완화가 중요한 값이라 허용.

## 5. 상세 캐시

`ProductService.cachedDetail` 을 Redis 에 10분 캐시. 수정·상태변경·찜은 `@CacheEvict`, 조회수는 캐시 밖에서 더한다.
캐시 대상에 "누가 봤는지"(liked, mine)를 넣지 않는 게 포인트 — 넣는 순간 사용자별 캐시가 되어 적중률이 무너진다.

## 6. 채팅 — N+1 과 브로드캐스트 타이밍

- 방 목록의 안 읽은 수는 방마다 COUNT 하지 않고 `GROUP BY room_id` 한 번 (`countUnreadByRoom`).
- 메시지 브로드캐스트는 **커밋 이후** (`TransactionSynchronization.afterCommit`). 커밋 전에 보내면 상대가 알림을 받고 REST 로 조회했을 때 아직 없을 수 있다.
- `chat_messages (room_id, sender_id) WHERE read_at IS NULL` 부분 인덱스: 안 읽은 메시지만 인덱싱해 크기를 작게 유지.

## 재현

```bash
docker compose up -d && ./gradlew bootRun     # V100 대량 시드 적용
bash scripts/explain-benchmark.sh
```
