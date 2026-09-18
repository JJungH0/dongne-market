package com.dongne.chat;

import com.dongne.chat.ChatDtos.*;
import com.dongne.common.ForbiddenException;
import com.dongne.common.NotFoundException;
import com.dongne.member.Member;
import com.dongne.member.MemberRepository;
import com.dongne.product.Product;
import com.dongne.product.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatService {

	private final ChatRoomRepository roomRepository;
	private final ChatMessageRepository messageRepository;
	private final ProductRepository productRepository;
	private final MemberRepository memberRepository;
	private final SimpMessagingTemplate messaging;

	public ChatService(ChatRoomRepository roomRepository, ChatMessageRepository messageRepository,
					   ProductRepository productRepository, MemberRepository memberRepository,
					   SimpMessagingTemplate messaging) {
		this.roomRepository = roomRepository;
		this.messageRepository = messageRepository;
		this.productRepository = productRepository;
		this.memberRepository = memberRepository;
		this.messaging = messaging;
	}

	/** 상품 상세의 "채팅하기": 이미 있으면 그 방, 없으면 생성. 판매자 본인은 자기 상품에 방을 못 연다. */
	@Transactional
	public Long openRoom(Long buyerId, Long productId) {
		Product product = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("상품을 찾을 수 없습니다."));
		if (product.getSellerId().equals(buyerId)) {
			throw new IllegalArgumentException("본인 상품에는 채팅을 시작할 수 없습니다.");
		}
		return roomRepository.findByProductIdAndBuyerId(productId, buyerId)
				.map(ChatRoom::getId)
				.orElseGet(() -> {
					ChatRoom room = roomRepository.save(new ChatRoom(productId, product.getSellerId(), buyerId));
					productRepository.incrementChatCount(productId);
					return room.getId();
				});
	}

	public ChatRoom getRoom(Long memberId, Long roomId) {
		ChatRoom room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("채팅방을 찾을 수 없습니다."));
		if (!room.isParticipant(memberId)) {
			throw new ForbiddenException("참여 중인 채팅방이 아닙니다.");
		}
		return room;
	}

	public boolean isParticipant(Long memberId, Long roomId) {
		return roomRepository.findById(roomId).map(r -> r.isParticipant(memberId)).orElse(false);
	}

	public List<RoomResponse> myRooms(Long memberId) {
		List<ChatRoom> rooms = roomRepository.findMyRooms(memberId);
		if (rooms.isEmpty()) return List.of();

		List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();
		Map<Long, Long> unread = messageRepository.countUnreadByRoom(roomIds, memberId).stream()
				.collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

		Set<Long> productIds = rooms.stream().map(ChatRoom::getProductId).collect(Collectors.toSet());
		Map<Long, Product> products = productRepository.findWithImagesByIdIn(productIds).stream()
				.collect(Collectors.toMap(Product::getId, p -> p));
		Set<Long> memberIds = rooms.stream().map(r -> r.counterpartOf(memberId)).collect(Collectors.toSet());
		Map<Long, Member> members = memberRepository.findAllById(memberIds).stream()
				.collect(Collectors.toMap(Member::getId, m -> m));

		return rooms.stream().map(r -> toResponse(r, memberId, products.get(r.getProductId()),
				members.get(r.counterpartOf(memberId)), unread.getOrDefault(r.getId(), 0L))).toList();
	}

	public RoomResponse roomDetail(Long memberId, Long roomId) {
		ChatRoom room = getRoom(memberId, roomId);
		Product product = productRepository.findWithImagesById(room.getProductId()).orElse(null);
		Member other = memberRepository.findById(room.counterpartOf(memberId)).orElse(null);
		long unread = messageRepository.countUnreadByRoom(List.of(roomId), memberId).stream()
				.mapToLong(r -> (Long) r[1]).sum();
		return toResponse(room, memberId, product, other, unread);
	}

	public List<MessageResponse> messages(Long memberId, Long roomId, Long beforeId, int size) {
		getRoom(memberId, roomId);
		return messageRepository.findPage(roomId, beforeId, PageRequest.of(0, Math.min(size, 100)))
				.stream().map(MessageResponse::from).toList();
	}

	/**
	 * 저장 → 방 갱신 → 브로드캐스트. 브로드캐스트는 커밋 이후에만 나간다.
	 * (커밋 전에 보내면 상대가 받자마자 REST 로 조회했을 때 아직 없을 수 있다)
	 */
	@Transactional
	public MessageResponse send(Long senderId, Long roomId, String content) {
		ChatRoom room = getRoom(senderId, roomId);
		ChatMessage saved = messageRepository.save(new ChatMessage(roomId, senderId, content.trim()));
		room.touch(saved.getContent(), saved.getCreatedAt());

		MessageResponse response = MessageResponse.from(saved);
		afterCommit(() -> {
			messaging.convertAndSend("/topic/rooms/" + roomId, RoomEvent.message(response));
			messaging.convertAndSend("/topic/users/" + room.counterpartOf(senderId), (Object) new UserEvent("ROOM_UPDATED", roomId));
		});
		return response;
	}

	@Transactional
	public int markRead(Long memberId, Long roomId) {
		getRoom(memberId, roomId);
		LocalDateTime now = LocalDateTime.now();
		int updated = messageRepository.markRead(roomId, memberId, now);
		if (updated > 0) {
			afterCommit(() -> messaging.convertAndSend("/topic/rooms/" + roomId, RoomEvent.read(memberId, now)));
		}
		return updated;
	}

	private static void afterCommit(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() { action.run(); }
			});
		} else {
			action.run();
		}
	}

	private static RoomResponse toResponse(ChatRoom r, Long me, Product p, Member other, long unread) {
		RoomProduct product = p == null ? null : new RoomProduct(p.getId(), p.getTitle(), p.getPrice(), p.getStatus().name(),
				p.getImages().isEmpty() ? null : p.getImages().getFirst().getUrl());
		RoomCounterpart counterpart = other == null ? null : new RoomCounterpart(other.getId(), other.getNickname(), other.getRegionName());
		return new RoomResponse(r.getId(), product, counterpart, r.getSellerId().equals(me),
				r.getLastMessage(), r.getLastMessageAt(), unread);
	}
}
