package com.dongne.member;

import com.dongne.support.ApiClient;
import com.dongne.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthApiTest {

	@Autowired MockMvc mvc;
	@Autowired ApiClient api;

	@Test
	void 회원가입_후_발급된_토큰으로_내_정보를_조회한다() throws Exception {
		String body = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"new@example.com","password":"password123","nickname":"뉴비","regionName":"역삼동","latitude":37.5,"longitude":127.03}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		String token = api.parse(body).get("accessToken").asString();

		mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("뉴비"))
				.andExpect(jsonPath("$.radiusKm").value(3));
	}

	@Test
	void 중복_이메일은_409() throws Exception {
		mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"minsu@example.com","password":"password123","nickname":"다른닉","regionName":"역삼동","latitude":37.5,"longitude":127.03}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void 시드_계정으로_로그인하고_틀린_비밀번호는_401() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"minsu@example.com\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.member.nickname").value("민수"));

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"minsu@example.com\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 토큰_없이_보호된_API는_401_공개_API는_200() throws Exception {
		mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/products?lat=37.5&lng=127.03&radiusKm=3")).andExpect(status().isOk());
	}

	@Test
	void 내_동네를_바꾸면_목록_기준점이_바뀐다() throws Exception {
		// 다른 테스트에 영향을 주지 않도록 새 회원으로 (역삼 가입 → 합정으로 이사)
		String signup = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"mover@example.com","password":"password123","nickname":"이사가는사람","regionName":"역삼동","latitude":37.5006,"longitude":127.0365}
						"""))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String token = api.parse(signup).get("accessToken").asString();

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/me/region")
						.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
						.content("{\"regionName\":\"합정동\",\"latitude\":37.5496,\"longitude\":126.9139,\"radiusKm\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.regionName").value("합정동"));

		String body = mvc.perform(get("/api/products").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode items = api.parse(body).get("items");
		assertThat(items).isNotEmpty();
		items.forEach(i -> assertThat(i.get("regionName").asString()).isEqualTo("합정동"));
	}
}
