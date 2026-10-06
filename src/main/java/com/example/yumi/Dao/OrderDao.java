package com.example.yumi.Dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.yumi.entity.Order;

public interface OrderDao extends JpaRepository<Order, Long> {

	Optional<Order> findByIdempotencyKey(UUID key);

	@Query("select o from Order o left join fetch o.items where o.order_id = :orderId")
	Optional<Order> findWithItemsById(@Param("orderId") String orderId);

}
