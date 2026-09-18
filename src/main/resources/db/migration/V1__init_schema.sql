CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 회원. 위치(위경도)와 반경으로 "내 동네"가 정해진다.
CREATE TABLE members (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(100) NOT NULL,
    nickname            VARCHAR(30)  NOT NULL UNIQUE,
    region_name         VARCHAR(50)  NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    radius_km           INT          NOT NULL DEFAULT 3,
    manner_temperature  NUMERIC(4,1) NOT NULL DEFAULT 36.5,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 중고 상품 게시글
CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    seller_id     BIGINT        NOT NULL REFERENCES members (id),
    title         VARCHAR(100)  NOT NULL,
    description   TEXT          NOT NULL,
    price         BIGINT        NOT NULL,
    category      VARCHAR(30)   NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    region_name   VARCHAR(50)   NOT NULL,
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    -- 위경도로부터 자동 계산되는 geography 컬럼. 애플리케이션은 위경도만 다루고 공간 연산은 DB가 맡는다.
    location      geography(Point, 4326) GENERATED ALWAYS AS
                  (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED,
    view_count    BIGINT        NOT NULL DEFAULT 0,
    like_count    INT           NOT NULL DEFAULT 0,
    chat_count    INT           NOT NULL DEFAULT 0,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    -- 끌어올리기(bump)로 갱신되는 정렬 기준
    bumped_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL
);
CREATE INDEX idx_product_images_product ON product_images (product_id, sort_order);

-- 찜
CREATE TABLE product_likes (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT    NOT NULL REFERENCES members (id),
    product_id  BIGINT    NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (member_id, product_id)
);
CREATE INDEX idx_product_likes_member ON product_likes (member_id, created_at DESC);

-- 채팅방: 상품 1개 + 구매 희망자 1명 = 방 1개
CREATE TABLE chat_rooms (
    id               BIGSERIAL PRIMARY KEY,
    product_id       BIGINT    NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    seller_id        BIGINT    NOT NULL REFERENCES members (id),
    buyer_id         BIGINT    NOT NULL REFERENCES members (id),
    last_message     VARCHAR(500),
    last_message_at  TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (product_id, buyer_id)
);
CREATE INDEX idx_chat_rooms_seller ON chat_rooms (seller_id, last_message_at DESC);
CREATE INDEX idx_chat_rooms_buyer  ON chat_rooms (buyer_id, last_message_at DESC);

CREATE TABLE chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT       NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    sender_id   BIGINT       NOT NULL REFERENCES members (id),
    content     VARCHAR(1000) NOT NULL,
    read_at     TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
-- 방 안에서 커서 페이징 (id 내림차순) + 안 읽은 메시지 집계
CREATE INDEX idx_chat_messages_room ON chat_messages (room_id, id DESC);
CREATE INDEX idx_chat_messages_unread ON chat_messages (room_id, sender_id) WHERE read_at IS NULL;
