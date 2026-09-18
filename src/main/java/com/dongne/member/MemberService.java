package com.dongne.member;

import com.dongne.common.ConflictException;
import com.dongne.common.NotFoundException;
import com.dongne.member.MemberDtos.*;
import com.dongne.security.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Transactional
	public TokenResponse signup(SignupRequest req) {
		if (memberRepository.existsByEmail(req.email())) throw new ConflictException("이미 사용 중인 이메일입니다.");
		if (memberRepository.existsByNickname(req.nickname())) throw new ConflictException("이미 사용 중인 닉네임입니다.");
		Member member = memberRepository.save(new Member(req.email(), passwordEncoder.encode(req.password()),
				req.nickname(), req.regionName(), req.latitude(), req.longitude()));
		return new TokenResponse(tokenService.issue(member), MemberResponse.from(member));
	}

	public TokenResponse login(LoginRequest req) {
		Member member = memberRepository.findByEmail(req.email())
				.orElseThrow(() -> new BadCredentialsException("invalid"));
		if (!passwordEncoder.matches(req.password(), member.getPassword())) {
			throw new BadCredentialsException("invalid");
		}
		return new TokenResponse(tokenService.issue(member), MemberResponse.from(member));
	}

	public Member get(Long id) {
		return memberRepository.findById(id).orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다: " + id));
	}

	@Transactional
	public MemberResponse updateRegion(Long id, UpdateRegionRequest req) {
		Member member = get(id);
		member.updateRegion(req.regionName(), req.latitude(), req.longitude(), req.radiusKm());
		return MemberResponse.from(member);
	}
}
