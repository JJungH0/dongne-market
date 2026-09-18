package com.dongne.security;

import com.dongne.member.Member;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

	private final JwtEncoder encoder;
	private final JwtProperties props;

	public TokenService(JwtEncoder encoder, JwtProperties props) {
		this.encoder = encoder;
		this.props = props;
	}

	public String issue(Member member) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("dongne-market")
				.issuedAt(now)
				.expiresAt(now.plus(props.accessTokenTtl()))
				.subject(String.valueOf(member.getId()))
				.claim("nickname", member.getNickname())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
