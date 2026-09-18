package com.dongne.like;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_likes")
public class ProductLike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected ProductLike() {}

	public ProductLike(Long memberId, Long productId) {
		this.memberId = memberId;
		this.productId = productId;
	}

	public Long getProductId() { return productId; }
}
