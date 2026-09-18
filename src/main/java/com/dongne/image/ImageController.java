package com.dongne.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * 로컬 디스크 업로드. 운영이라면 S3 presigned URL 로 바꿔 서버를 거치지 않게 하는 지점.
 */
@Tag(name = "Image")
@RestController
@RequestMapping("/api/images")
public class ImageController {

	private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

	private final Path uploadDir;

	public ImageController(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
		this.uploadDir = Path.of(uploadDir).toAbsolutePath();
		Files.createDirectories(this.uploadDir);
	}

	public record UploadResponse(String url) {}

	@Operation(summary = "이미지 업로드 (5MB 이하, jpeg/png/webp/gif)")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
		if (file.isEmpty() || file.getContentType() == null || !ALLOWED.contains(file.getContentType())) {
			throw new IllegalArgumentException("jpeg/png/webp/gif 이미지만 업로드할 수 있습니다.");
		}
		String ext = switch (file.getContentType()) {
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			case "image/gif" -> "gif";
			default -> "jpg";
		};
		String name = UUID.randomUUID() + "." + ext;   // 원본 파일명은 신뢰하지 않는다 (경로 조작 방지)
		Files.copy(file.getInputStream(), uploadDir.resolve(name));
		return new UploadResponse("/uploads/" + name);
	}
}
