package com.dongne.chat;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.security.Principal;
import java.util.List;

/**
 * STOMP 프레임 단위 인증/인가.
 * - CONNECT: Authorization 헤더의 JWT 를 검증해 세션 사용자로 등록 (HTTP 필터는 WebSocket 프레임을 못 본다)
 * - SUBSCRIBE: 남의 방/남의 개인 토픽은 구독 불가 → 예외 = ERROR 프레임 후 연결 종료
 */
public class StompAuthInterceptor implements ChannelInterceptor {

	private final JwtDecoder jwtDecoder;
	private final ChatService chatService;

	public StompAuthInterceptor(JwtDecoder jwtDecoder, ChatService chatService) {
		this.jwtDecoder = jwtDecoder;
		this.chatService = chatService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) return message;

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String bearer = accessor.getFirstNativeHeader("Authorization");
			if (bearer == null || !bearer.startsWith("Bearer ")) {
				throw new IllegalArgumentException("인증 토큰이 없습니다.");
			}
			try {
				Jwt jwt = jwtDecoder.decode(bearer.substring(7));
				accessor.setUser(new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, List.of()));
			} catch (JwtException e) {
				throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
			}
		}

		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			Principal user = accessor.getUser();
			String dest = accessor.getDestination();
			if (user == null || dest == null) throw new IllegalArgumentException("인증이 필요합니다.");
			Long memberId = Long.valueOf(user.getName());

			if (dest.startsWith("/topic/rooms/")) {
				Long roomId = Long.valueOf(dest.substring("/topic/rooms/".length()));
				if (!chatService.isParticipant(memberId, roomId)) throw new IllegalArgumentException("참여 중인 채팅방이 아닙니다.");
			} else if (dest.startsWith("/topic/users/")) {
				if (!dest.equals("/topic/users/" + memberId)) throw new IllegalArgumentException("다른 사용자의 채널은 구독할 수 없습니다.");
			} else {
				throw new IllegalArgumentException("허용되지 않는 구독 대상입니다.");
			}
		}
		return message;
	}
}
