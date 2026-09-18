package com.dongne.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

	@Query("select r from ChatRoom r where r.sellerId = :memberId or r.buyerId = :memberId order by r.lastMessageAt desc nulls last, r.createdAt desc")
	List<ChatRoom> findMyRooms(Long memberId);
}
