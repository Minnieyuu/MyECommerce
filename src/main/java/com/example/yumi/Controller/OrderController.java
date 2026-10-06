package com.example.yumi.Controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.yumi.Service.OrderService;
import com.example.yumi.dto.CreateOrderRequest;
import com.example.yumi.dto.GetOrderDto;
import com.example.yumi.dto.OrderResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController {

	@Autowired
	OrderService orderService;

	@PostMapping
	public ResponseEntity<OrderResponse> create(
			@RequestHeader("Idempotency-Key") UUID idempotencyKey,
			@Valid @RequestBody CreateOrderRequest request) {

		OrderResponse response = orderService.create(idempotencyKey, request);

		int httpStatus = Integer.parseInt(response.getCode());

		return ResponseEntity.status(httpStatus).body(response);
	}

	/**
	 * 訂單查詢，查無訂單OR非本人訂單 回傳空物件
	 * 
	 * @param userId
	 * @param orderId
	 * @return
	 */
	@GetMapping("/{orderId}")
	public GetOrderDto get(
			@RequestHeader @NotBlank @Size(max = 64) String userId,
			@PathVariable String orderId) {

		return orderService.getOrder(userId, orderId);
	}

}
