package com.dongne.product;

import com.dongne.common.ForbiddenException;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private long price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductStatus status = ProductStatus.ON_SALE;

	@Column(name = "region_name", nullable = false)
	private String regionName;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

	// location(geography) 컬럼은 DB 생성 컬럼이라 엔티티에 매핑하지 않는다. 공간 연산은 네이티브 쿼리에서만.

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "like_count", nullable = false)
	private int likeCount;

	@Column(name = "chat_count", nullable = false)
	private int chatCount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "bumped_at", nullable = false)
	private LocalDateTime bumpedAt = LocalDateTime.now();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<ProductImage> images = new ArrayList<>();

	protected Product() {
	}

	public Product(Long sellerId, String title, String description, long price, Category category,
				   String regionName, double latitude, double longitude, List<String> imageUrls) {
		this.sellerId = sellerId;
		this.title = title;
		this.description = description;
		this.price = price;
		this.category = category;
		this.regionName = regionName;
		this.latitude = latitude;
		this.longitude = longitude;
		replaceImages(imageUrls);
	}

	public void update(Long requesterId, String title, String description, long price, Category category, List<String> imageUrls) {
		assertOwner(requesterId);
		this.title = title;
		this.description = description;
		this.price = price;
		this.category = category;
		replaceImages(imageUrls);
	}

	public void changeStatus(Long requesterId, ProductStatus next) {
		assertOwner(requesterId);
		this.status = next;
	}

	/** 끌어올리기: 정렬 기준만 갱신. 하루 1회 제한은 서비스에서. */
	public void bump(Long requesterId) {
		assertOwner(requesterId);
		this.bumpedAt = LocalDateTime.now();
	}

	public void assertOwner(Long requesterId) {
		if (!sellerId.equals(requesterId)) {
			throw new ForbiddenException("본인이 올린 상품만 수정할 수 있습니다.");
		}
	}

	private void replaceImages(List<String> urls) {
		images.clear();
		int order = 0;
		for (String url : urls) {
			images.add(new ProductImage(this, url, order++));
		}
	}

	public Long getId() { return id; }
	public Long getSellerId() { return sellerId; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public long getPrice() { return price; }
	public Category getCategory() { return category; }
	public ProductStatus getStatus() { return status; }
	public String getRegionName() { return regionName; }
	public double getLatitude() { return latitude; }
	public double getLongitude() { return longitude; }
	public long getViewCount() { return viewCount; }
	public int getLikeCount() { return likeCount; }
	public int getChatCount() { return chatCount; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getBumpedAt() { return bumpedAt; }
	public List<ProductImage> getImages() { return images; }
}
