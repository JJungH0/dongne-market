package com.dongne.member;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class MemberDtos {
	private MemberDtos() {}

	public record SignupRequest(
			@NotBlank @Email String email,
			@NotBlank @Size(min = 8, max = 64) String password,
			@NotBlank @Size(min = 2, max = 20) String nickname,
			@NotBlank String regionName,
			@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
			@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {
	}

	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
	}

	public record TokenResponse(String accessToken, MemberResponse member) {
	}

	public record UpdateRegionRequest(
			@NotBlank String regionName,
			@NotNull Double latitude,
			@NotNull Double longitude,
			@NotNull @Min(1) @Max(20) Integer radiusKm) {
	}

	public record MemberResponse(Long id, String email, String nickname, String regionName,
								 double latitude, double longitude, int radiusKm, BigDecimal mannerTemperature) {
		public static MemberResponse from(Member m) {
			return new MemberResponse(m.getId(), m.getEmail(), m.getNickname(), m.getRegionName(),
					m.getLatitude(), m.getLongitude(), m.getRadiusKm(), m.getMannerTemperature());
		}
	}

	/** 다른 사용자에게 보여주는 공개 프로필 (이메일·위치 제외) */
	public record PublicProfile(Long id, String nickname, String regionName, BigDecimal mannerTemperature) {
		public static PublicProfile from(Member m) {
			return new PublicProfile(m.getId(), m.getNickname(), m.getRegionName(), m.getMannerTemperature());
		}
	}
}
