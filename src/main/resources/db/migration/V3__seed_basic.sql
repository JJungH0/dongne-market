-- 비밀번호는 모두 'password123' (bcrypt)
INSERT INTO members (id, email, password, nickname, region_name, latitude, longitude, radius_km, manner_temperature) VALUES
    (1, 'minsu@example.com',  '$2a$10$Jpfv9vC8NtBiuCtWEMn.4e8zH/hOT1lxnwltb0CLynFOo8E/LsLvW', '민수',   '역삼동', 37.5006, 127.0365, 3, 41.2),
    (2, 'yuna@example.com',   '$2a$10$Jpfv9vC8NtBiuCtWEMn.4e8zH/hOT1lxnwltb0CLynFOo8E/LsLvW', '유나',   '서초동', 37.4837, 127.0324, 3, 38.7),
    (3, 'jihoon@example.com', '$2a$10$Jpfv9vC8NtBiuCtWEMn.4e8zH/hOT1lxnwltb0CLynFOo8E/LsLvW', '지훈',   '성수동', 37.5445, 127.0560, 5, 36.5),
    (4, 'sora@example.com',   '$2a$10$Jpfv9vC8NtBiuCtWEMn.4e8zH/hOT1lxnwltb0CLynFOo8E/LsLvW', '소라',   '합정동', 37.5496, 126.9139, 3, 45.1);
SELECT setval('members_id_seq', 4);

INSERT INTO products (id, seller_id, title, description, price, category, status, region_name, latitude, longitude, view_count, like_count, created_at, bumped_at) VALUES
    (1, 1, '아이패드 에어 5세대 64GB 스타라이트', '작년 12월 구매, 케이스 끼고 써서 기스 없어요. 애플펜슬 2세대 포함입니다. 역삼역 근처 직거래 선호.', 650000, 'DIGITAL', 'ON_SALE', '역삼동', 37.5012, 127.0371, 152, 9, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 hours'),
    (2, 1, '이케아 책상 린몬 120x60', '이사 가서 팝니다. 상판 약간 얼룩 있음(사진 참고). 직접 가져가셔야 해요.', 20000, 'FURNITURE', 'ON_SALE', '역삼동', 37.4998, 127.0359, 88, 3, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    (3, 2, '다이슨 에어랩 컴플리트', '선물 받았는데 안 써서 내놔요. 박스 풀세트. 서초역 3번 출구 직거래.', 380000, 'BEAUTY', 'ON_SALE', '서초동', 37.4840, 127.0330, 431, 27, NOW() - INTERVAL '1 day', NOW() - INTERVAL '30 minutes'),
    (4, 2, '닌텐도 스위치 OLED + 젤다 왕눈', '조이콘 쏠림 없음. 젤다 티어스 오브 더 킹덤 칩 포함.', 320000, 'DIGITAL', 'RESERVED', '서초동', 37.4831, 127.0318, 210, 14, NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day'),
    (5, 3, '캠핑 의자 2개 + 테이블 세트', '헬리녹스 아님, 국산 브랜드. 세 번 사용. 성수동 직거래.', 45000, 'SPORTS', 'ON_SALE', '성수동', 37.5448, 127.0565, 64, 5, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    (6, 3, '자라 여성 트렌치코트 S', '한 번 입음. 베이지. 택 있음.', 35000, 'CLOTHING', 'SOLD', '성수동', 37.5441, 127.0552, 300, 18, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    (7, 4, '맥북 프로 14 M3 Pro 18GB/512GB', '작업용으로 3개월 사용. 사이클 40회. 충전기 박스 포함. 합정역 근처.', 2200000, 'DIGITAL', 'ON_SALE', '합정동', 37.5499, 126.9142, 890, 41, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
    (8, 4, '무민 머그컵 2개', '아라비아 핀란드 정품. 새 제품.', 30000, 'LIVING', 'ON_SALE', '합정동', 37.5493, 126.9135, 40, 2, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    (9, 1, '에어팟 프로 2 (USB-C)', '왼쪽 유닛 배터리 조금 빨리 닳아요. 그래서 싸게.', 150000, 'DIGITAL', 'ON_SALE', '역삼동', 37.5020, 127.0380, 122, 6, NOW() - INTERVAL '1 day', NOW() - INTERVAL '3 hours'),
    (10, 2, '반려견 유모차 (소형견용)', '3kg 말티즈 태우던 거예요. 세척 완료.', 60000, 'PET', 'ON_SALE', '서초동', 37.4850, 127.0340, 55, 4, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days');
SELECT setval('products_id_seq', 10);

INSERT INTO product_images (product_id, url, sort_order) VALUES
    (1, 'https://picsum.photos/seed/ipad1/800/800', 0), (1, 'https://picsum.photos/seed/ipad2/800/800', 1),
    (2, 'https://picsum.photos/seed/desk/800/800', 0),
    (3, 'https://picsum.photos/seed/dyson/800/800', 0), (3, 'https://picsum.photos/seed/dyson2/800/800', 1),
    (4, 'https://picsum.photos/seed/switch/800/800', 0),
    (5, 'https://picsum.photos/seed/camp/800/800', 0),
    (6, 'https://picsum.photos/seed/coat/800/800', 0),
    (7, 'https://picsum.photos/seed/mbp/800/800', 0), (7, 'https://picsum.photos/seed/mbp2/800/800', 1), (7, 'https://picsum.photos/seed/mbp3/800/800', 2),
    (8, 'https://picsum.photos/seed/mug/800/800', 0),
    (9, 'https://picsum.photos/seed/airpods/800/800', 0),
    (10, 'https://picsum.photos/seed/dog/800/800', 0);

INSERT INTO product_likes (member_id, product_id) VALUES (2, 1), (3, 1), (1, 3), (4, 3), (1, 7);

INSERT INTO chat_rooms (id, product_id, seller_id, buyer_id, last_message, last_message_at) VALUES
    (1, 1, 1, 2, '내일 저녁 7시 역삼역 괜찮으세요?', NOW() - INTERVAL '20 minutes'),
    (2, 3, 2, 1, '네고 조금 가능할까요?', NOW() - INTERVAL '2 hours');
SELECT setval('chat_rooms_id_seq', 2);

INSERT INTO chat_messages (room_id, sender_id, content, read_at, created_at) VALUES
    (1, 2, '안녕하세요! 아이패드 아직 판매 중인가요?', NOW() - INTERVAL '50 minutes', NOW() - INTERVAL '1 hour'),
    (1, 1, '네 판매 중이에요 :)', NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '45 minutes'),
    (1, 2, '내일 저녁 7시 역삼역 괜찮으세요?', NULL, NOW() - INTERVAL '20 minutes'),
    (2, 1, '에어랩 관심 있어요. 네고 조금 가능할까요?', NULL, NOW() - INTERVAL '2 hours');
