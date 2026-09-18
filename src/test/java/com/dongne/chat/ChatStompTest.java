package com.dongne.chat;

import com.dongne.chat.ChatDtos.RoomEvent;
import com.dongne.support.ApiClient;
import com.dongne.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진짜 WebSocket 으로 붙어서: JWT CONNECT → 방 구독 → /app 전송 → 상대가 /topic 으로 수신.
 */
@IntegrationTest
class ChatStompTest {

	@LocalServerPort int port;
	@Autowired ApiClient api;

	private StompSession connect(long memberId) throws Exception {
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		client.setMessageConverter(new JacksonJsonMessageConverter());
		StompHeaders headers = new StompHeaders();
		headers.add("Authorization", "Bearer " + api.tokenOf(memberId));
		return client.connectAsync("ws://localhost:" + port + "/ws", new org.springframework.web.socket.WebSocketHttpHeaders(),
				headers, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
	}

	@Test
	void 상대가_보낸_메시지를_방_토픽으로_실시간_수신한다() throws Exception {
		StompSession seller = connect(1);
		StompSession buyer = connect(2);
		BlockingQueue<RoomEvent> received = new LinkedBlockingQueue<>();

		buyer.subscribe("/topic/rooms/1", new StompFrameHandler() {
			public Type getPayloadType(StompHeaders h) { return RoomEvent.class; }
			public void handleFrame(StompHeaders h, Object payload) { received.add((RoomEvent) payload); }
		});
		Thread.sleep(300);   // 구독 등록 대기

		seller.send("/app/chat/1", Map.of("content", "실시간 안녕"));

		RoomEvent event = received.poll(5, TimeUnit.SECONDS);
		assertThat(event).isNotNull();
		assertThat(event.type()).isEqualTo("MESSAGE");
		assertThat(event.message().senderId()).isEqualTo(1L);
		assertThat(event.message().content()).isEqualTo("실시간 안녕");

		seller.disconnect();
		buyer.disconnect();
	}

	@Test
	void 토큰_없이_CONNECT_하면_연결이_거부된다() {
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		client.setMessageConverter(new JacksonJsonMessageConverter());
		var future = client.connectAsync("ws://localhost:" + port + "/ws", new org.springframework.web.socket.WebSocketHttpHeaders(),
				new StompHeaders(), new StompSessionHandlerAdapter() {});
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
				.isInstanceOf(ExecutionException.class);
	}

	@Test
	void 참여하지_않은_방을_구독하면_ERROR_프레임을_받는다() throws Exception {
		CompletableFuture<Throwable> error = new CompletableFuture<>();
		WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
		client.setMessageConverter(new JacksonJsonMessageConverter());
		StompHeaders headers = new StompHeaders();
		headers.add("Authorization", "Bearer " + api.tokenOf(3));   // 지훈은 방 1의 참여자가 아님
		StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new org.springframework.web.socket.WebSocketHttpHeaders(),
				headers, new StompSessionHandlerAdapter() {
					@Override
					public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) { error.complete(ex); }
					@Override
					public void handleTransportError(StompSession s, Throwable ex) { error.complete(ex); }
				}).get(5, TimeUnit.SECONDS);

		session.subscribe("/topic/rooms/1", new StompFrameHandler() {
			public Type getPayloadType(StompHeaders h) { return RoomEvent.class; }
			public void handleFrame(StompHeaders h, Object payload) {}
		});

		assertThat(error.get(5, TimeUnit.SECONDS)).isNotNull();
	}
}
