package com.dongne.like;

import com.dongne.product.Product;
import com.dongne.product.ProductRepository;
import com.dongne.product.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LikeService {

	private final ProductLikeRepository likeRepository;
	private final ProductRepository productRepository;

	public LikeService(ProductLikeRepository likeRepository, ProductRepository productRepository) {
		this.likeRepository = likeRepository;
		this.productRepository = productRepository;
	}

	/**
	 * 토글. 같은 사용자가 더블클릭해도 UNIQUE(member,product) 가 두 번째 INSERT 를 막고,
	 * like_count 는 원자적 UPDATE 라 동시 토글에도 실제 행 수와 어긋나지 않는다.
	 *
	 * @return 토글 후 찜 상태
	 */
	@Transactional
	@CacheEvict(cacheNames = ProductService.DETAIL_CACHE, key = "#productId")
	public boolean toggle(Long memberId, Long productId) {
		productRepository.findById(productId).orElseThrow(() -> new com.dongne.common.NotFoundException("상품을 찾을 수 없습니다."));
		var existing = likeRepository.findByMemberIdAndProductId(memberId, productId);
		if (existing.isPresent()) {
			likeRepository.delete(existing.get());
			productRepository.addLikeCount(productId, -1);
			return false;
		}
		try {
			likeRepository.saveAndFlush(new ProductLike(memberId, productId));
		} catch (DataIntegrityViolationException e) {
			return true;   // 동시 요청이 먼저 넣음 → 이미 찜 상태
		}
		productRepository.addLikeCount(productId, 1);
		return true;
	}

	@Transactional(readOnly = true)
	public boolean isLiked(Long memberId, Long productId) {
		return memberId != null && likeRepository.existsByMemberIdAndProductId(memberId, productId);
	}

	@Transactional(readOnly = true)
	public List<Product> myLikes(Long memberId) {
		List<Long> ids = likeRepository.findProductIdsByMemberId(memberId);
		var byId = productRepository.findWithImagesByIdIn(ids).stream().collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
		return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
	}
}
