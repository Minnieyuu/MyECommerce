package com.example.yumi.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.yumi.Enum.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Digits;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "Orders", uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;

	@Column(name = "order_id", unique = true, nullable = true, length = 64)
	private String orderId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.CREATED;

	@Digits(integer = 13, fraction = 5)
	@Column(name = "total_amount", length = 40)
	private BigDecimal totalAmount;

	@Column(name = "create_date", nullable = false)
	private Instant createdAt;

	// 用於驗證重複下單--key
	@Column(name = "idempotency_key", nullable = false, length = 36)
	private UUID idempotencyKey;

	// 用於驗證重複下單--request body
	@Column(name = "request_hash", nullable = false, length = 64)
	private String requestHash;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

}
