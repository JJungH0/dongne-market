package com.dongne.product;

import com.dongne.common.CursorPage;
import com.dongne.common.NotFoundException;
import com.dongne.member.Member;
import com.dongne.member.MemberRepository;
import com.dongne.product.ProductDtos.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

	public static final String DETAIL_CACHE = "productDetail";

	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;
	private final ViewCounter viewCounter;

	public ProductService(ProductRepository productRepository, MemberRepository memberRepository, ViewCounter viewCounter) {
		this.productRepository = productRepository;
		this.memberRepository = memberRepository;
		this.viewCounter = viewCounter;
	}

	/** 판매자의 동네·위치를 상품에 스냅샷으로 복사한다 (판매자가 이사해도 글 위치는 유지) */
	@Transactional
	public Long create(Long sellerId, CreateRequest req) {
		Member seller = memberRepository.findById(sellerId).orElseThrow(() -> new NotFoundException("회원 없음"));
		Product product = new Product(sellerId, req.title(), req.description(), req.price(), req.category(),
				seller.getRegionName(), seller.getLatitude(), seller.getLongitude(), req.imageUrlsOrEmpty());
		return productRepository.save(product).getId();
	}

	@Transactional
	@CacheEvict(cacheNames = DETAIL_CACHE, key = "#productId")
	public void update(Long requesterId, Long productId, CreateRequest req) {
		Product product = productRepository.findWithImagesById(productId).orElseThrow(this::notFound);
		product.update(requesterId, req.title(), req.description(), req.price(), req.category(), req.imageUrlsOrEmpty());
	}

	@Transactional
	@CacheEvict(cacheNames = DETAIL_CACHE, key = "#productId")
	public void changeStatus(Long requesterId, Long productId, ProductStatus status) {
		productRepository.findById(productId).orElseThrow(this::notFound).changeStatus(requesterId, status);
	}

	@Transactional
	@CacheEvict(cacheNames = DETAIL_CACHE, key = "#productId")
	public void bump(Long requesterId, Long productId) {
		Product product = productRepository.findById(productId).orElseThrow(this::notFound);
		if (product.getBumpedAt().isAfter(LocalDateTime.now().minus(Duration.ofDays(1)))) {
			throw new IllegalArgumentException("끌어올리기는 하루에 한 번만 할 수 있습니다.");
		}
		product.bump(requesterId);
	}

	@Transactional
	@CacheEvict(cacheNames = DETAIL_CACHE, key = "#productId")
	public void delete(Long requesterId, Long productId) {
		Product product = productRepository.findById(productId).orElseThrow(this::notFound);
		product.assertOwner(requesterId);
		productRepository.delete(product);
	}

	public CursorPage<ProductSummary> search(ProductSearchCondition condition) {
		return productRepository.search(condition);
	}

	public List<Product> mySales(Long sellerId) {
		return productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
	}

	/**
	 * 상세: 캐시된 본문 + Redis 에 아직 반영 안 된 조회수. 조회수 증가는 캐시 밖에서 처리.
	 */
	public ProductDetail detail(Long productId, String viewerKey) {
		viewCounter.count(productId, viewerKey);
		ProductDetail cached = cachedDetail(productId);
		return cached.withViewCount(cached.viewCount() + viewCounter.pending(productId));
	}

	@Cacheable(cacheNames = DETAIL_CACHE, key = "#productId")
	public ProductDetail cachedDetail(Long productId) {
		Product product = productRepository.findWithImagesById(productId).orElseThrow(this::notFound);
		Member seller = memberRepository.findById(product.getSellerId()).orElseThrow(() -> new NotFoundException("판매자 없음"));
		return ProductDetail.from(product, seller, product.getViewCount());
	}

	public Product get(Long productId) {
		return productRepository.findById(productId).orElseThrow(this::notFound);
	}

	private NotFoundException notFound() {
		return new NotFoundException("상품을 찾을 수 없습니다.");
	}
}
