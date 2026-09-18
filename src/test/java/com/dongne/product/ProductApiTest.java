package com.dongne.product;

import com.dongne.support.ApiClient;
import com.dongne.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProductApiTest {

	@Autowired MockMvc mvc;
	@Autowired ApiClient api;

	private JsonNode list(String query) throws Exception {
		String body = mvc.perform(get("/api/products" + query)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return api.parse(body);
	}

	@Test
	void 반경_3km_안의_상품만_나오고_거리가_함께_온다() throws Exception {
		// 역삼동 기준 3km: 역삼·서초는 포함, 성수(≈5km)·합정(≈11km)은 제외
		JsonNode page = list("?lat=37.5006&lng=127.0365&radiusKm=3&includeSold=true");
		Set<String> regions = new HashSet<>();
		page.get("items").forEach(i -> {
			regions.add(i.get("regionName").asString());
			assertThat(i.get("distanceMeters").asDouble()).isLessThanOrEqualTo(3000);
		});
		assertThat(regions).containsExactlyInAnyOrder("역삼동", "서초동");

		JsonNode wide = list("?lat=37.5006&lng=127.0365&radiusKm=20&includeSold=true");
		assertThat(wide.get("items").size()).isEqualTo(10);
	}

	@Test
	void 커서_페이징은_중복도_누락도_없이_끝까지_간다() throws Exception {
		List<Long> ids = new ArrayList<>();
		String cursor = null;
		int pages = 0;
		do {
			JsonNode page = list("?lat=37.5006&lng=127.0365&radiusKm=20&includeSold=true&size=3" + (cursor == null ? "" : "&cursor=" + cursor));
			page.get("items").forEach(i -> ids.add(i.get("id").asLong()));
			cursor = page.get("hasNext").asBoolean() ? page.get("nextCursor").asString() : null;
			pages++;
		} while (cursor != null);

		assertThat(pages).isEqualTo(4);   // 10건 / 3 = 3.33 → 4페이지
		assertThat(ids).hasSize(10).doesNotHaveDuplicates();
		assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
	}

	@Test
	void 잘못된_커서는_400() throws Exception {
		mvc.perform(get("/api/products?cursor=not-a-cursor")).andExpect(status().isBadRequest());
	}

	@Test
	void 키워드_카테고리_필터와_판매완료_기본_제외() throws Exception {
		JsonNode byKeyword = list("?keyword=맥북&includeSold=true");
		assertThat(byKeyword.get("items")).hasSize(1);
		assertThat(byKeyword.get("items").get(0).get("title").asString()).contains("맥북");

		JsonNode digital = list("?category=DIGITAL&includeSold=true");
		digital.get("items").forEach(i -> assertThat(i.get("category").asString()).isEqualTo("DIGITAL"));

		JsonNode onSale = list("");
		onSale.get("items").forEach(i -> assertThat(i.get("status").asString()).isNotEqualTo("SOLD"));
	}

	@Test
	void 등록_수정_상태변경_삭제는_본인만() throws Exception {
		String body = mvc.perform(api.postJson("/api/products", """
						{"title":"테스트 상품","description":"설명","price":10000,"category":"ETC","imageUrls":["/uploads/a.jpg"]}
						""", 1))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long id = api.parse(body).get("id").asLong();

		// 판매자 위치가 스냅샷으로 복사됨
		mvc.perform(get("/api/products/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.product.regionName").value("역삼동"))
				.andExpect(jsonPath("$.product.imageUrls[0]").value("/uploads/a.jpg"))
				.andExpect(jsonPath("$.product.seller.nickname").value("민수"));

		String update = "{\"title\":\"수정됨\",\"description\":\"설명\",\"price\":9000,\"category\":\"ETC\",\"imageUrls\":[]}";
		mvc.perform(api.putJson("/api/products/" + id, update, 2)).andExpect(status().isForbidden());
		mvc.perform(api.putJson("/api/products/" + id, update, 1)).andExpect(status().isOk());

		mvc.perform(api.patchJson("/api/products/" + id + "/status", "{\"status\":\"SOLD\"}", 2)).andExpect(status().isForbidden());
		mvc.perform(api.patchJson("/api/products/" + id + "/status", "{\"status\":\"SOLD\"}", 1)).andExpect(status().isOk());
		mvc.perform(get("/api/products/" + id))
				.andExpect(jsonPath("$.product.title").value("수정됨"))
				.andExpect(jsonPath("$.product.status").value("SOLD"));

		mvc.perform(api.authed(delete("/api/products/" + id), 2)).andExpect(status().isForbidden());
		mvc.perform(api.authed(delete("/api/products/" + id), 1)).andExpect(status().isNoContent());
		mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void 끌어올리기는_하루_한_번() throws Exception {
		// 시드 2번 상품은 5일 전에 끌어올려짐 → 가능, 직후 재시도 → 400
		mvc.perform(api.authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/products/2/bump"), 1)).andExpect(status().isOk());
		mvc.perform(api.authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/products/2/bump"), 1)).andExpect(status().isBadRequest());

		JsonNode page = list("?lat=37.5006&lng=127.0365&radiusKm=3");
		assertThat(page.get("items").get(0).get("id").asLong()).isEqualTo(2L);   // 맨 위로 올라옴
	}

	@Test
	void 찜_토글은_상태와_카운트를_함께_바꾼다() throws Exception {
		mvc.perform(api.postJson("/api/products/5/like", "", 1)).andExpect(jsonPath("$.liked").value(true));
		mvc.perform(get("/api/products/5").header("Authorization", "Bearer " + api.tokenOf(1)))
				.andExpect(jsonPath("$.liked").value(true))
				.andExpect(jsonPath("$.product.likeCount").value(6));

		mvc.perform(api.postJson("/api/products/5/like", "", 1)).andExpect(jsonPath("$.liked").value(false));
		mvc.perform(get("/api/products/5").header("Authorization", "Bearer " + api.tokenOf(1)))
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.product.likeCount").value(5));

		mvc.perform(api.getAs("/api/me/likes", 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.containsInAnyOrder(3, 7)));
	}
}
