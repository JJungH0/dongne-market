package com.dongne.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductSearchRepository {

	@EntityGraph(attributePaths = "images")
	Optional<Product> findWithImagesById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id = :id")
	Optional<Product> findForUpdate(Long id);

	/** 찜 수는 동시 토글에도 정확해야 하므로 엔티티를 읽어 +1 하지 않고 원자적 UPDATE */
	@Modifying
	@Query("update Product p set p.likeCount = p.likeCount + :delta where p.id = :id")
	void addLikeCount(Long id, int delta);

	@Modifying
	@Query("update Product p set p.chatCount = p.chatCount + 1 where p.id = :id")
	void incrementChatCount(Long id);

	@Modifying
	@Query(value = "update products set view_count = view_count + :delta where id = :id", nativeQuery = true)
	void addViewCount(Long id, long delta);

	@EntityGraph(attributePaths = "images")
	List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

	@EntityGraph(attributePaths = "images")
	List<Product> findWithImagesByIdIn(java.util.Collection<Long> ids);
}
