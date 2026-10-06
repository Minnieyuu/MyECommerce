package com.example.yumi.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.yumi.entity.OrderItem;

import lombok.Data;

@Data
public class OrderItemDto {

	private Long productId;

	private String productName;

	private BigDecimal unitPrice;

	private int quantity;

	public static List<OrderItemDto> parse(List<OrderItem> items) {

		List<OrderItemDto> result = new ArrayList<>();

		for (OrderItem item : items) {
			OrderItemDto dto = new OrderItemDto();
			dto.setProductId(item.getProductId());
			dto.setProductName(item.getProductName());
			dto.setUnitPrice(item.getUnitPrice());
			dto.setQuantity(item.getQuantity());
			result.add(dto);
		}

		return result;
	}

}
