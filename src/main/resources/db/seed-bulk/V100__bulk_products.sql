-- 성능 측정용 대량 데이터. 서울 시청(37.5665, 126.9780) 반경 약 15km 에 무작위 분포.
INSERT INTO products (seller_id, title, description, price, category, status, region_name, latitude, longitude, view_count, like_count, created_at, bumped_at)
SELECT
    1 + (g % 4),
    (ARRAY['아이폰 15 프로', '갤럭시 S24', '에어팟', '자전거', '전자레인지', '책상', '의자', '원피스', '운동화', '캠핑 텐트', '강아지 사료', '유모차', '모니터 27인치', '키보드', '에어프라이어', '냉장고 소형', '패딩', '골프채', '기타', '텀블러'])[1 + (g % 20)]
        || ' ' || (ARRAY['팝니다', '급처', '거의 새것', '네고 가능', '직거래만', '박스 풀셋'])[1 + (g % 6)] || ' #' || g,
    '대량 시드 데이터 #' || g,
    (1 + (g % 200)) * 5000,
    (ARRAY['DIGITAL','FURNITURE','CLOTHING','SPORTS','LIVING','BEAUTY','PET','BOOK','ETC'])[1 + (g % 9)],
    CASE WHEN g % 10 = 0 THEN 'SOLD' WHEN g % 15 = 0 THEN 'RESERVED' ELSE 'ON_SALE' END,
    (ARRAY['역삼동','서초동','성수동','합정동','잠실동','상계동','목동','신촌동','종로','용산동'])[1 + (g % 10)],
    37.5665 + (random() - 0.5) * 0.30,
    126.9780 + (random() - 0.5) * 0.38,
    (random() * 500)::bigint,
    (random() * 30)::int,
    NOW() - (random() * INTERVAL '60 days'),
    NOW() - (random() * INTERVAL '60 days')
FROM generate_series(1, 20000) AS g;

ANALYZE products;
