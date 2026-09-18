package com.dongne.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	/** 최신 메시지부터, beforeId 보다 작은 것만 (키셋). Pageable 은 limit 용도로만. */
	@Query("select m from ChatMessage m where m.roomId = :roomId and (:beforeId is null or m.id < :beforeId) order by m.id desc")
	List<ChatMessage> findPage(Long roomId, Long beforeId, Pageable pageable);

	/** 방별 안 읽은 메시지 수를 한 번에 (N+1 방지) */
	@Query("select m.roomId, count(m) from ChatMessage m where m.roomId in :roomIds and m.senderId <> :me and m.readAt is null group by m.roomId")
	List<Object[]> countUnreadByRoom(List<Long> roomIds, Long me);

	@Modifying
	@Query("update ChatMessage m set m.readAt = :now where m.roomId = :roomId and m.senderId <> :me and m.readAt is null")
	int markRead(Long roomId, Long me, LocalDateTime now);
}
