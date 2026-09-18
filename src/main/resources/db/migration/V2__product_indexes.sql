-- 위치 반경 검색: ST_DWithin 이 GIST 인덱스를 탄다
CREATE INDEX idx_products_location ON products USING GIST (location);

-- 목록 기본 정렬(끌어올린 순) + 커서 페이징 (bumped_at, id) 키셋
CREATE INDEX idx_products_status_bumped ON products (status, bumped_at DESC, id DESC);

-- 제목 부분 일치 검색: LIKE '%키워드%' 는 B-tree 를 못 타므로 trigram GIN
CREATE INDEX idx_products_title_trgm ON products USING GIN (title gin_trgm_ops);

-- 판매자별 목록 (프로필 / 내 판매 내역)
CREATE INDEX idx_products_seller ON products (seller_id, created_at DESC);
