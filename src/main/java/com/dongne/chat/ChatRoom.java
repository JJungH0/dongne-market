package com.dongne.chat;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "buyer_id", nullable = false)
	private Long buyerId;

	@Column(name = "last_message")
	private String lastMessage;

	@Column(name = "last_message_at")
	private LocalDateTime lastMessageAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected ChatRoom() {}

	public ChatRoom(Long productId, Long sellerId, Long buyerId) {
		this.productId = productId;
		this.sellerId = sellerId;
		this.buyerId = buyerId;
	}

	public boolean isParticipant(Long memberId) {
		return sellerId.equals(memberId) || buyerId.equals(memberId);
	}

	public Long counterpartOf(Long memberId) {
		return sellerId.equals(memberId) ? buyerId : sellerId;
	}

	public void touch(String preview, LocalDateTime at) {
		this.lastMessage = preview.length() > 500 ? preview.substring(0, 500) : preview;
		this.lastMessageAt = at;
	}

	public Long getId() { return id; }
	public Long getProductId() { return productId; }
	public Long getSellerId() { return sellerId; }
	public Long getBuyerId() { return buyerId; }
	public String getLastMessage() { return lastMessage; }
	public LocalDateTime getLastMessageAt() { return lastMessageAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
