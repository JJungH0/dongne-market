package com.dongne.chat;

import com.dongne.support.ApiClient;
import com.dongne.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ChatApiTest {

	@Autowired MockMvc mvc;
	@Autowired ApiClient api;

	@Test
	void 채팅방_열기는_멱등이고_본인_상품에는_열_수_없다() throws Exception {
		// 지훈(3)이 민수(1)의 아이패드(1)에 채팅
		String first = mvc.perform(api.authed(post("/api/chat/rooms?productId=1"), 3)).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String second = mvc.perform(api.authed(post("/api/chat/rooms?productId=1"), 3)).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		assertThat(api.parse(first).get("roomId").asLong()).isEqualTo(api.parse(second).get("roomId").asLong());

		mvc.perform(api.authed(post("/api/chat/rooms?productId=1"), 1)).andExpect(status().isBadRequest());
	}

	@Test
	void 메시지_전송_안읽음_집계_읽음처리() throws Exception {
		// 시드 방 1: 아이패드, 판매자 민수(1), 구매자 유나(2). 유나의 마지막 메시지를 민수가 안 읽음
		String rooms = mvc.perform(api.getAs("/api/chat/rooms", 1)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode room1 = api.parse(rooms).get(0);
		assertThat(room1.get("id").asLong()).isEqualTo(1L);
		assertThat(room1.get("unreadCount").asLong()).isEqualTo(1);
		assertThat(room1.get("counterpart").get("nickname").asString()).isEqualTo("유나");
		assertThat(room1.get("iAmSeller").asBoolean()).isTrue();

		mvc.perform(api.postJson("/api/chat/rooms/1/read", "", 1))
				.andExpect(jsonPath("$.updated").value(1));
		mvc.perform(api.getAs("/api/chat/rooms/1", 1))
				.andExpect(jsonPath("$.unreadCount").value(0));

		mvc.perform(api.postJson("/api/chat/rooms/1/messages", "{\"content\":\"네 좋아요!\"}", 1))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.senderId").value(1));
		mvc.perform(api.getAs("/api/chat/rooms/1", 2))
				.andExpect(jsonPath("$.unreadCount").value(1))
				.andExpect(jsonPath("$.lastMessage").value("네 좋아요!"));

		// 메시지 목록은 최신순 + 커서
		String msgs = mvc.perform(api.getAs("/api/chat/rooms/1/messages?size=2", 2)).andReturn().getResponse().getContentAsString();
		JsonNode list = api.parse(msgs);
		assertThat(list).hasSize(2);
		assertThat(list.get(0).get("content").asString()).isEqualTo("네 좋아요!");
		long beforeId = list.get(1).get("id").asLong();
		String older = mvc.perform(api.getAs("/api/chat/rooms/1/messages?size=10&beforeId=" + beforeId, 2)).andReturn().getResponse().getContentAsString();
		assertThat(api.parse(older)).hasSize(2);
	}

	@Test
	void 참여자가_아니면_403() throws Exception {
		mvc.perform(api.getAs("/api/chat/rooms/1/messages", 3)).andExpect(status().isForbidden());
		mvc.perform(api.postJson("/api/chat/rooms/1/messages", "{\"content\":\"몰래\"}", 3)).andExpect(status().isForbidden());
	}
}
