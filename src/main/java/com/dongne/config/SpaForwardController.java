package com.dongne.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** React Router 경로로 직접 진입/새로고침해도 index.html 을 내려준다 (API·정적 파일은 제외) */
@Controller
public class SpaForwardController {

	@GetMapping({"/login", "/signup", "/write", "/chat", "/chat/{id:\\d+}", "/me", "/region",
			"/products/{id:\\d+}", "/products/{id:\\d+}/edit"})
	public String forward() {
		return "forward:/index.html";
	}
}
