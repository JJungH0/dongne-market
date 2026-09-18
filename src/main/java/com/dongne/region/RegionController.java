package com.dongne.region;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 지도 없이 동네를 고를 수 있게 제공하는 프리셋. 실서비스라면 행정동 DB + 역지오코딩. */
@Tag(name = "Region")
@RestController
@RequestMapping("/api/regions")
public class RegionController {

	public record Region(String name, double latitude, double longitude) {}

	public static final List<Region> PRESETS = List.of(
			new Region("역삼동", 37.5006, 127.0365),
			new Region("서초동", 37.4837, 127.0324),
			new Region("성수동", 37.5445, 127.0560),
			new Region("합정동", 37.5496, 126.9139),
			new Region("잠실동", 37.5133, 127.1001),
			new Region("상계동", 37.6605, 127.0731),
			new Region("목동",   37.5262, 126.8642),
			new Region("신촌동", 37.5558, 126.9368),
			new Region("종로",   37.5720, 126.9794),
			new Region("용산동", 37.5311, 126.9810));

	@Operation(summary = "동네 프리셋 목록")
	@GetMapping
	public List<Region> list() {
		return PRESETS;
	}
}
