package com.example.yumi.dto;

import java.math.BigDecimal;

import com.example.yumi.Enum.ProductStatus;
import com.example.yumi.entity.Product;

public record ProductResponse(Long id, String name, BigDecimal price, int stock, ProductStatus status) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getPrice(),
				product.getStock(), product.getStatus());
	}

}
