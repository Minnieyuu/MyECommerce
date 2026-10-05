package com.example.yumi.Dao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.yumi.entity.Order;

public interface OrderDao extends JpaRepository<Order, Long> {

	Optional<Order> findByIdempotencyKey(UUID key);

}
