package com.dongne.product;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 조회수는 상세 조회마다 DB UPDATE 를 치면 인기 상품의 같은 행에 락 경합이 생긴다.
 * Redis INCR 로 받아뒀다가 주기적으로 한 번에 DB 에 반영한다 (write-behind).
 * 같은 사용자의 반복 조회는 1시간 동안 1회만 센다.
 */
@Component
public class ViewCounter {

	static final String PENDING_PREFIX = "product:view:";
	static final String SEEN_PREFIX = "product:seen:";

	private final StringRedisTemplate redis;
	private final ProductRepository productRepository;

	public ViewCounter(StringRedisTemplate redis, ProductRepository productRepository) {
		this.redis = redis;
		this.productRepository = productRepository;
	}

	/** @param viewerKey 회원 ID 또는 비회원 식별자(IP) */
	public void count(Long productId, String viewerKey) {
		Boolean first = redis.opsForValue().setIfAbsent(SEEN_PREFIX + productId + ":" + viewerKey, "1", Duration.ofHours(1));
		if (Boolean.TRUE.equals(first)) {
			redis.opsForValue().increment(PENDING_PREFIX + productId);
		}
	}

	public long pending(Long productId) {
		String v = redis.opsForValue().get(PENDING_PREFIX + productId);
		return v == null ? 0 : Long.parseLong(v);
	}

	@Scheduled(fixedDelayString = "${app.view-flush-interval:30s}")
	@Transactional
	public void flush() {
		List<String> keys = new ArrayList<>();
		try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(PENDING_PREFIX + "*").count(200).build())) {
			cursor.forEachRemaining(keys::add);
		}
		for (String key : keys) {
			String v = redis.opsForValue().getAndDelete(key);   // 읽고 지우는 사이의 증가분은 다음 flush 로
			if (v == null) continue;
			long productId = Long.parseLong(key.substring(PENDING_PREFIX.length()));
			productRepository.addViewCount(productId, Long.parseLong(v));
		}
	}
}
