package com.dongne.product;

import java.time.LocalDateTime;

/** 목록 카드 1장에 필요한 만큼만 (설명·이미지 전체는 상세에서) */
public record ProductSummary(
		Long id,
		String title,
		long price,
		String category,
		String status,
		String regionName,
		String thumbnailUrl,
		int likeCount,
		int chatCount,
		long viewCount,
		Double distanceMeters,
		LocalDateTime bumpedAt
) {
}
