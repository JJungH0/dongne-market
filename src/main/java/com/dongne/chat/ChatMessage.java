package com.dongne.chat;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "sender_id", nullable = false)
	private Long senderId;

	@Column(nullable = false)
	private String content;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected ChatMessage() {}

	public ChatMessage(Long roomId, Long senderId, String content) {
		this.roomId = roomId;
		this.senderId = senderId;
		this.content = content;
	}

	public Long getId() { return id; }
	public Long getRoomId() { return roomId; }
	public Long getSenderId() { return senderId; }
	public String getContent() { return content; }
	public LocalDateTime getReadAt() { return readAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
