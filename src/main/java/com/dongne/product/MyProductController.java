package com.dongne.product;

import com.dongne.like.LikeService;
import com.dongne.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "My")
@RestController
@RequestMapping("/api/me")
public class MyProductController {

	private final ProductService productService;
	private final LikeService likeService;

	public MyProductController(ProductService productService, LikeService likeService) {
		this.productService = productService;
		this.likeService = likeService;
	}

	@Operation(summary = "내 판매 상품")
	@GetMapping("/products")
	public List<ProductSummary> mySales(@CurrentMember Long memberId) {
		return productService.mySales(memberId).stream().map(MyProductController::summary).toList();
	}

	@Operation(summary = "내가 찜한 상품")
	@GetMapping("/likes")
	public List<ProductSummary> myLikes(@CurrentMember Long memberId) {
		return likeService.myLikes(memberId).stream().map(MyProductController::summary).toList();
	}

	static ProductSummary summary(Product p) {
		String thumb = p.getImages().isEmpty() ? null : p.getImages().getFirst().getUrl();
		return new ProductSummary(p.getId(), p.getTitle(), p.getPrice(), p.getCategory().name(), p.getStatus().name(),
				p.getRegionName(), thumb, p.getLikeCount(), p.getChatCount(), p.getViewCount(), null, p.getBumpedAt());
	}
}
