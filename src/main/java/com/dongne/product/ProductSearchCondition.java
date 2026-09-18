package com.dongne.product;

/**
 * @param latitude/longitude/radiusKm 기준점과 반경 (없으면 위치 필터 없음)
 * @param cursor                      "bumpedAtEpochMillis_id" 를 base64url 로 인코딩한 키셋 커서
 */
public record ProductSearchCondition(
		Double latitude,
		Double longitude,
		Integer radiusKm,
		Category category,
		String keyword,
		boolean includeSold,
		String cursor,
		int size
) {
}
