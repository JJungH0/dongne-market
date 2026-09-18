package com.dongne.product;

import com.dongne.common.CursorPage;
import com.dongne.like.LikeService;
import com.dongne.member.Member;
import com.dongne.member.MemberService;
import com.dongne.product.ProductDtos.*;
import com.dongne.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Product")
@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;
	private final LikeService likeService;
	private final MemberService memberService;

	public ProductController(ProductService productService, LikeService likeService, MemberService memberService) {
		this.productService = productService;
		this.likeService = likeService;
		this.memberService = memberService;
	}

	@Operation(summary = "상품 목록 (내 동네 반경 + 커서 페이징). 로그인 시 위치 생략하면 내 동네 기준")
	@GetMapping
	public CursorPage<ProductSummary> list(
			@CurrentMember Long memberId,
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lng,
			@RequestParam(required = false) Integer radiusKm,
			@RequestParam(required = false) Category category,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "false") boolean includeSold,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") int size) {

		if ((lat == null || lng == null) && memberId != null) {
			Member me = memberService.get(memberId);
			lat = me.getLatitude();
			lng = me.getLongitude();
			if (radiusKm == null) radiusKm = me.getRadiusKm();
		}
		if (lat != null && radiusKm == null) radiusKm = 3;
		int pageSize = Math.min(Math.max(size, 1), 50);
		return productService.search(new ProductSearchCondition(lat, lng, radiusKm, category, keyword, includeSold, cursor, pageSize));
	}

	@Operation(summary = "상품 상세 (조회수 집계 포함)")
	@GetMapping("/{id}")
	public ProductDetailResponse detail(@CurrentMember Long memberId, @PathVariable Long id, HttpServletRequest request) {
		String viewerKey = memberId != null ? "m" + memberId : "ip" + request.getRemoteAddr();
		ProductDetail detail = productService.detail(id, viewerKey);
		return new ProductDetailResponse(detail, likeService.isLiked(memberId, id),
				memberId != null && memberId.equals(detail.seller().id()));
	}

	@Operation(summary = "상품 등록")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> create(@CurrentMember Long memberId, @Valid @RequestBody CreateRequest req) {
		return Map.of("id", productService.create(memberId, req));
	}

	@Operation(summary = "상품 수정 (본인만)")
	@PutMapping("/{id}")
	public void update(@CurrentMember Long memberId, @PathVariable Long id, @Valid @RequestBody CreateRequest req) {
		productService.update(memberId, id, req);
	}

	@Operation(summary = "판매 상태 변경 (판매중/예약중/거래완료)")
	@PatchMapping("/{id}/status")
	public void changeStatus(@CurrentMember Long memberId, @PathVariable Long id, @Valid @RequestBody StatusRequest req) {
		productService.changeStatus(memberId, id, req.status());
	}

	@Operation(summary = "끌어올리기 (하루 1회)")
	@PostMapping("/{id}/bump")
	public void bump(@CurrentMember Long memberId, @PathVariable Long id) {
		productService.bump(memberId, id);
	}

	@Operation(summary = "삭제 (본인만)")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@CurrentMember Long memberId, @PathVariable Long id) {
		productService.delete(memberId, id);
	}

	@Operation(summary = "찜 토글")
	@PostMapping("/{id}/like")
	public Map<String, Boolean> like(@CurrentMember Long memberId, @PathVariable Long id) {
		return Map.of("liked", likeService.toggle(memberId, id));
	}

	@Operation(summary = "카테고리 목록")
	@GetMapping("/categories")
	public List<Map<String, String>> categories() {
		return java.util.Arrays.stream(Category.values()).map(c -> Map.of("code", c.name(), "label", c.getLabel())).toList();
	}
}
