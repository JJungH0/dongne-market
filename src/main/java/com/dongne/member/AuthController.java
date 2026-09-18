package com.dongne.member;

import com.dongne.member.MemberDtos.*;
import com.dongne.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api")
public class AuthController {

	private final MemberService memberService;

	public AuthController(MemberService memberService) {
		this.memberService = memberService;
	}

	@Operation(summary = "회원가입 (가입 즉시 토큰 발급)")
	@PostMapping("/auth/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse signup(@Valid @RequestBody SignupRequest req) {
		return memberService.signup(req);
	}

	@Operation(summary = "로그인")
	@PostMapping("/auth/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest req) {
		return memberService.login(req);
	}

	@Operation(summary = "내 정보")
	@GetMapping("/me")
	public MemberResponse me(@CurrentMember Long memberId) {
		return MemberResponse.from(memberService.get(memberId));
	}

	@Operation(summary = "내 동네(위치·반경) 변경")
	@PutMapping("/me/region")
	public MemberResponse updateRegion(@CurrentMember Long memberId, @Valid @RequestBody UpdateRegionRequest req) {
		return memberService.updateRegion(memberId, req);
	}
}
