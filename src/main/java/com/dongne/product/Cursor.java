package com.dongne.product;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

/** 키셋 커서 인코딩/디코딩. 클라이언트에겐 불투명한 문자열. */
final class Cursor {

	private Cursor() {}

	record Key(LocalDateTime bumpedAt, long id) {}

	static String encode(LocalDateTime bumpedAt, long id) {
		String raw = bumpedAt.toInstant(ZoneOffset.UTC).toEpochMilli() + "_" + id;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	static Key decode(String cursor) {
		try {
			String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			String[] parts = raw.split("_");
			LocalDateTime ts = LocalDateTime.ofEpochSecond(Long.parseLong(parts[0]) / 1000,
					(int) (Long.parseLong(parts[0]) % 1000) * 1_000_000, ZoneOffset.UTC);
			return new Key(ts, Long.parseLong(parts[1]));
		} catch (Exception e) {
			throw new IllegalArgumentException("잘못된 커서입니다.");
		}
	}
}
