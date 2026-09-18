package com.dongne.support;

import com.dongne.member.Member;
import com.dongne.member.MemberRepository;
import com.dongne.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 테스트용 얇은 헬퍼: 시드 회원 토큰 발급 + JSON 파싱 */
@Component
public class ApiClient {

	@Autowired MockMvc mvc;
	@Autowired TokenService tokenService;
	@Autowired MemberRepository memberRepository;
	@Autowired JsonMapper json;

	public String tokenOf(long memberId) {
		Member m = memberRepository.findById(memberId).orElseThrow();
		return tokenService.issue(m);
	}

	public MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder b, long memberId) {
		return b.header("Authorization", "Bearer " + tokenOf(memberId));
	}

	public MockHttpServletRequestBuilder getAs(String url, long memberId) {
		return authed(get(url), memberId);
	}

	public MockHttpServletRequestBuilder postJson(String url, String body, long memberId) {
		return authed(post(url).contentType(MediaType.APPLICATION_JSON).content(body), memberId);
	}

	public MockHttpServletRequestBuilder putJson(String url, String body, long memberId) {
		return authed(put(url).contentType(MediaType.APPLICATION_JSON).content(body), memberId);
	}

	public MockHttpServletRequestBuilder patchJson(String url, String body, long memberId) {
		return authed(patch(url).contentType(MediaType.APPLICATION_JSON).content(body), memberId);
	}

	public JsonNode parse(String body) {
		return json.readTree(body);
	}
}
