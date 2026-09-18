#!/usr/bin/env bash
# 인덱스 유무 · 페이징 방식별 EXPLAIN ANALYZE 비교 (docker compose 의 postgres 대상, 대량 시드 적용 상태에서)
set -euo pipefail
psql() { docker exec dongne-postgres psql -U dongne -d dongne -tA -c "$1"; }
ex() { psql "EXPLAIN (ANALYZE, BUFFERS) $1" | grep -E "Execution Time|Seq Scan|Index Scan|Bitmap Index Scan|Index Only" | head -3; }
RADIUS="ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(127.0365,37.5006),4326)::geography, 3000)"

echo "=== 1. 반경 3km 검색 — GIST 있음"
ex "SELECT p.id FROM products p WHERE $RADIUS AND p.status <> 'SOLD' ORDER BY p.bumped_at DESC, p.id DESC LIMIT 21"
psql "DROP INDEX idx_products_location" >/dev/null
echo "=== 1. 반경 3km 검색 — GIST 없음"
ex "SELECT p.id FROM products p WHERE $RADIUS AND p.status <> 'SOLD' ORDER BY p.bumped_at DESC, p.id DESC LIMIT 21"
psql "CREATE INDEX idx_products_location ON products USING GIST (location)" >/dev/null

echo; echo "=== 2. 제목 ILIKE '%갤럭시%' — trigram GIN 있음"
ex "SELECT p.id FROM products p WHERE p.title ILIKE '%갤럭시%' AND p.status <> 'SOLD' ORDER BY p.bumped_at DESC, p.id DESC LIMIT 21"
psql "DROP INDEX idx_products_title_trgm" >/dev/null
echo "=== 2. 제목 ILIKE '%갤럭시%' — 인덱스 없음"
ex "SELECT p.id FROM products p WHERE p.title ILIKE '%갤럭시%' AND p.status <> 'SOLD' ORDER BY p.bumped_at DESC, p.id DESC LIMIT 21"
psql "CREATE INDEX idx_products_title_trgm ON products USING GIN (title gin_trgm_ops)" >/dev/null

echo; echo "=== 3. 전체 목록 900페이지째 (18,000건 뒤 20건)"
echo "--- OFFSET 18000"
ex "SELECT p.id FROM products p WHERE p.status <> 'SOLD' ORDER BY p.bumped_at DESC, p.id DESC OFFSET 18000 LIMIT 21"
CUR=$(psql "SELECT bumped_at || '|' || id FROM products WHERE status <> 'SOLD' ORDER BY bumped_at DESC, id DESC OFFSET 17999 LIMIT 1")
TS=${CUR%|*}; ID=${CUR#*|}
echo "--- 키셋 커서 (bumped_at, id) < ('$TS', $ID) — 복합 인덱스 있음"
ex "SELECT p.id FROM products p WHERE p.status <> 'SOLD' AND (p.bumped_at, p.id) < ('$TS', $ID) ORDER BY p.bumped_at DESC, p.id DESC LIMIT 21"
psql "DROP INDEX idx_products_status_bumped" >/dev/null
echo "--- 키셋 커서 — 복합 인덱스 없음"
ex "SELECT p.id FROM products p WHERE p.status <> 'SOLD' AND (p.bumped_at, p.id) < ('$TS', $ID) ORDER BY p.bumped_at DESC, p.id DESC LIMIT 21"
psql "CREATE INDEX idx_products_status_bumped ON products (status, bumped_at DESC, id DESC)" >/dev/null
