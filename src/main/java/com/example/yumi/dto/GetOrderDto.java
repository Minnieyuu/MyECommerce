package com.example.yumi.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.yumi.Enum.OrderStatus;
import com.example.yumi.entity.Order;

import lombok.Data;

@Data
public class GetOrderDto {

	// 訂單編號
	private String orderId;

	// 訂單狀態
	private OrderStatus status;

	// 訂單總金額
	private BigDecimal totalAmount;

	// 訂單細項
	private List<OrderItemDto> items;

	public static GetOrderDto parse(Order order) {

		GetOrderDto dto = new GetOrderDto();

		dto.setOrderId(order.getOrderId());
		dto.setStatus(order.getStatus());
		dto.setTotalAmount(order.getTotalAmount());
		dto.setItems(OrderItemDto.parse(order.getItems()));

		return dto;
	}

}
