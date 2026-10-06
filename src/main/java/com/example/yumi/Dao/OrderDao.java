package com.example.yumi.Dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.yumi.entity.Order;

import jakarta.persistence.LockModeType;

public interface OrderDao extends JpaRepository<Order, Long> {

	Optional<Order> findByIdempotencyKey(UUID key);

	// 查詢訂單用
	@Query("select o from Order o left join fetch o.items where o.orderId = :orderId")
	Optional<Order> findWithItemsById(@Param("orderId") String orderId);

	// 付款OR取消用，鎖住直到transactional完成
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
	Optional<Order> findByOrderIdForUpdate(@Param("orderId") String orderId);

}
