package com.dongne.product;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private String url;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected ProductImage() {
	}

	public ProductImage(Product product, String url, int sortOrder) {
		this.product = product;
		this.url = url;
		this.sortOrder = sortOrder;
	}

	public String getUrl() { return url; }
	public int getSortOrder() { return sortOrder; }
}
