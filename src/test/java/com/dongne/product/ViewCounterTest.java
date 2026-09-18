package com.dongne.product;

import com.dongne.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ViewCounterTest {

	@Autowired ViewCounter viewCounter;
	@Autowired ProductRepository productRepository;
	@Autowired ProductService productService;

	@Test
	void 같은_사용자의_반복_조회는_한_번만_세고_flush_후_DB에_합산된다() {
		long before = productRepository.findById(8L).orElseThrow().getViewCount();

		viewCounter.count(8L, "m1");
		viewCounter.count(8L, "m1");
		viewCounter.count(8L, "m2");
		assertThat(viewCounter.pending(8L)).isEqualTo(2);

		// 상세 응답은 DB 값 + 미반영분
		assertThat(productService.detail(8L, "m3").viewCount()).isEqualTo(before + 3);

		viewCounter.flush();
		assertThat(viewCounter.pending(8L)).isZero();
		assertThat(productRepository.findById(8L).orElseThrow().getViewCount()).isEqualTo(before + 3);
	}
}
