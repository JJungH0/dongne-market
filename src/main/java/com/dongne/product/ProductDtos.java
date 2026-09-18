package com.dongne.product;

import com.dongne.member.Member;
import com.dongne.member.MemberDtos.PublicProfile;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public final class ProductDtos {
	private ProductDtos() {}

	public record CreateRequest(
			@NotBlank @Size(max = 100) String title,
			@NotBlank @Size(max = 5000) String description,
			@NotNull @Min(0) @Max(1_000_000_000) Long price,
			@NotNull Category category,
			@Size(max = 10) List<@NotBlank String> imageUrls) {
		public List<String> imageUrlsOrEmpty() { return imageUrls == null ? List.of() : imageUrls; }
	}

	public record StatusRequest(@NotNull ProductStatus status) {
	}

	public record ProductDetail(
			Long id, String title, String description, long price, String category, String categoryLabel,
			String status, String regionName, double latitude, double longitude,
			List<String> imageUrls, long viewCount, int likeCount, int chatCount,
			LocalDateTime createdAt, LocalDateTime bumpedAt, PublicProfile seller) {

		public static ProductDetail from(Product p, Member seller, long viewCount) {
			return new ProductDetail(p.getId(), p.getTitle(), p.getDescription(), p.getPrice(),
					p.getCategory().name(), p.getCategory().getLabel(), p.getStatus().name(),
					p.getRegionName(), p.getLatitude(), p.getLongitude(),
					p.getImages().stream().map(ProductImage::getUrl).toList(),
					viewCount, p.getLikeCount(), p.getChatCount(), p.getCreatedAt(), p.getBumpedAt(),
					PublicProfile.from(seller));
		}

		public ProductDetail withViewCount(long v) {
			return new ProductDetail(id, title, description, price, category, categoryLabel, status, regionName,
					latitude, longitude, imageUrls, v, likeCount, chatCount, createdAt, bumpedAt, seller);
		}
	}

	/** 상세 + 요청자 관점 정보 */
	public record ProductDetailResponse(ProductDetail product, boolean liked, boolean mine) {
	}
}
