package com.dongne.product;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorTest {

	@Test
	void 인코딩_디코딩_왕복() {
		LocalDateTime ts = LocalDateTime.of(2026, 9, 18, 12, 34, 56, 789_000_000);
		Cursor.Key key = Cursor.decode(Cursor.encode(ts, 42L));
		assertThat(key.bumpedAt()).isEqualTo(ts);
		assertThat(key.id()).isEqualTo(42L);
	}

	@Test
	void 깨진_커서는_IllegalArgumentException() {
		assertThatThrownBy(() -> Cursor.decode("???")).isInstanceOf(IllegalArgumentException.class);
	}
}
