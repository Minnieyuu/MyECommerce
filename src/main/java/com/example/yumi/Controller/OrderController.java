package com.example.yumi.Controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.yumi.Service.OrderService;
import com.example.yumi.dto.CreateOrderRequest;
import com.example.yumi.dto.OrderResponse;

import jakarta.validation.Valid;
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

}
