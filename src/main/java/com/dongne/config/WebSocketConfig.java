package com.dongne.config;

import com.dongne.chat.ChatService;
import com.dongne.chat.StompAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 단일 인스턴스 → 인메모리 심플 브로커. 인스턴스가 늘면 enableStompBrokerRelay(RabbitMQ/Redis) 로 교체하는 지점.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtDecoder jwtDecoder;
	private final ChatService chatService;

	public WebSocketConfig(JwtDecoder jwtDecoder, @Lazy ChatService chatService) {
		this.jwtDecoder = jwtDecoder;
		this.chatService = chatService;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new StompAuthInterceptor(jwtDecoder, chatService));
	}
}
