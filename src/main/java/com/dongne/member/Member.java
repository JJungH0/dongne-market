package com.dongne.member;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, unique = true)
	private String nickname;

	@Column(name = "region_name", nullable = false)
	private String regionName;

	@Column(nullable = false)
	private double latitude;

	@Column(nullable = false)
	private double longitude;

	@Column(name = "radius_km", nullable = false)
	private int radiusKm = 3;

	@Column(name = "manner_temperature", nullable = false)
	private BigDecimal mannerTemperature = new BigDecimal("36.5");

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	protected Member() {
	}

	public Member(String email, String encodedPassword, String nickname, String regionName, double latitude, double longitude) {
		this.email = email;
		this.password = encodedPassword;
		this.nickname = nickname;
		this.regionName = regionName;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void updateRegion(String regionName, double latitude, double longitude, int radiusKm) {
		if (radiusKm < 1 || radiusKm > 20) {
			throw new IllegalArgumentException("반경은 1~20km 사이여야 합니다.");
		}
		this.regionName = regionName;
		this.latitude = latitude;
		this.longitude = longitude;
		this.radiusKm = radiusKm;
	}

	public Long getId() { return id; }
	public String getEmail() { return email; }
	public String getPassword() { return password; }
	public String getNickname() { return nickname; }
	public String getRegionName() { return regionName; }
	public double getLatitude() { return latitude; }
	public double getLongitude() { return longitude; }
	public int getRadiusKm() { return radiusKm; }
	public BigDecimal getMannerTemperature() { return mannerTemperature; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
