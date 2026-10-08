package com.example.yumi.Controller.Api;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
	public OrderResponse create(
			@RequestHeader("Idempotency-Key") UUID idempotencyKey,
			@Valid @RequestBody CreateOrderRequest request) {

		return orderService.create(idempotencyKey, request);
	}

	/**
	 * 訂單查詢，查無訂單OR非本人訂單 回傳404
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

	/**
	 * 付款
	 */
	@PostMapping("/{orderId}/pay")
	public OrderResponse pay(
			@RequestHeader @NotBlank @Size(max = 64) String userId,
			@PathVariable String orderId) {

		return orderService.pay(userId, orderId);
	}

	/**
	 * 取消訂單
	 * 
	 * @param userId
	 * @param orderId
	 * @return
	 */
	@PostMapping("/{orderId}/cancel")
	public OrderResponse cancel(
			@RequestHeader @NotBlank @Size(max = 64) String userId,
			@PathVariable String orderId) {

		return orderService.cancel(userId, orderId);
	}

}
