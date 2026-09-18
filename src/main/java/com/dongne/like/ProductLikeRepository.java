package com.dongne.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {
	Optional<ProductLike> findByMemberIdAndProductId(Long memberId, Long productId);
	boolean existsByMemberIdAndProductId(Long memberId, Long productId);

	@Query("select l.productId from ProductLike l where l.memberId = :memberId order by l.createdAt desc")
	List<Long> findProductIdsByMemberId(Long memberId);
}
