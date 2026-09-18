package com.dongne.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ChatDtos {
	private ChatDtos() {}

	public record SendRequest(@NotBlank @Size(max = 1000) String content) {}

	public record MessageResponse(Long id, Long roomId, Long senderId, String content, LocalDateTime createdAt, LocalDateTime readAt) {
		public static MessageResponse from(ChatMessage m) {
			return new MessageResponse(m.getId(), m.getRoomId(), m.getSenderId(), m.getContent(), m.getCreatedAt(), m.getReadAt());
		}
	}

	public record RoomProduct(Long id, String title, long price, String status, String thumbnailUrl) {}

	public record RoomCounterpart(Long id, String nickname, String regionName) {}

	public record RoomResponse(Long id, RoomProduct product, RoomCounterpart counterpart, boolean iAmSeller,
							   String lastMessage, LocalDateTime lastMessageAt, long unreadCount) {}

	/** 개인 토픽(/topic/users/{id}) 이벤트: 방 목록 갱신 신호 */
	public record UserEvent(String type, Long roomId) {}

	/** 방 토픽으로 흘러가는 이벤트. type: MESSAGE | READ */
	public record RoomEvent(String type, MessageResponse message, Long readerId, LocalDateTime readAt) {
		public static RoomEvent message(MessageResponse m) { return new RoomEvent("MESSAGE", m, null, null); }
		public static RoomEvent read(Long readerId, LocalDateTime at) { return new RoomEvent("READ", null, readerId, at); }
	}
}
