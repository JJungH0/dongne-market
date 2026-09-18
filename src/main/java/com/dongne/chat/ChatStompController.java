package com.dongne.chat;

import com.dongne.chat.ChatDtos.SendRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/** 클라이언트 → /app/chat/{roomId} 로 보내면 저장 후 /topic/rooms/{roomId} 로 브로드캐스트 */
@Controller
public class ChatStompController {

	private final ChatService chatService;

	public ChatStompController(ChatService chatService) {
		this.chatService = chatService;
	}

	@MessageMapping("/chat/{roomId}")
	public void send(@DestinationVariable Long roomId, @Valid @Payload SendRequest req, Principal principal) {
		chatService.send(Long.valueOf(principal.getName()), roomId, req.content());
	}
}
