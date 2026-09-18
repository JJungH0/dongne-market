package com.dongne.common;

import java.util.List;

/**
 * 커서 기반 페이지. OFFSET 페이징은 뒤로 갈수록 느려지고(앞 페이지를 다 읽고 버림),
 * 새 글이 끼어들면 중복/누락이 생기므로 무한 스크롤에는 키셋 커서를 쓴다.
 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

	public static <T> CursorPage<T> of(List<T> fetched, int size, java.util.function.Function<T, String> cursorOf) {
		boolean hasNext = fetched.size() > size;
		List<T> items = hasNext ? fetched.subList(0, size) : fetched;
		String next = hasNext ? cursorOf.apply(items.getLast()) : null;
		return new CursorPage<>(items, next, hasNext);
	}
}
