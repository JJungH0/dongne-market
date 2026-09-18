package com.dongne.chat;

import com.dongne.chat.ChatDtos.*;
import com.dongne.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Chat")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@Operation(summary = "상품에 대한 채팅방 열기 (있으면 기존 방)")
	@PostMapping("/rooms")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> open(@CurrentMember Long memberId, @RequestParam Long productId) {
		return Map.of("roomId", chatService.openRoom(memberId, productId));
	}

	@Operation(summary = "내 채팅방 목록 (안 읽은 수 포함)")
	@GetMapping("/rooms")
	public List<RoomResponse> rooms(@CurrentMember Long memberId) {
		return chatService.myRooms(memberId);
	}

	@Operation(summary = "채팅방 정보")
	@GetMapping("/rooms/{roomId}")
	public RoomResponse room(@CurrentMember Long memberId, @PathVariable Long roomId) {
		return chatService.roomDetail(memberId, roomId);
	}

	@Operation(summary = "메시지 목록 (최신순, beforeId 커서)")
	@GetMapping("/rooms/{roomId}/messages")
	public List<MessageResponse> messages(@CurrentMember Long memberId, @PathVariable Long roomId,
										  @RequestParam(required = false) Long beforeId,
										  @RequestParam(defaultValue = "50") int size) {
		return chatService.messages(memberId, roomId, beforeId, size);
	}

	@Operation(summary = "메시지 전송 (REST 폴백. 실시간은 STOMP /app/chat/{roomId})")
	@PostMapping("/rooms/{roomId}/messages")
	@ResponseStatus(HttpStatus.CREATED)
	public MessageResponse send(@CurrentMember Long memberId, @PathVariable Long roomId, @Valid @RequestBody SendRequest req) {
		return chatService.send(memberId, roomId, req.content());
	}

	@Operation(summary = "읽음 처리")
	@PostMapping("/rooms/{roomId}/read")
	public Map<String, Integer> read(@CurrentMember Long memberId, @PathVariable Long roomId) {
		return Map.of("updated", chatService.markRead(memberId, roomId));
	}
}
