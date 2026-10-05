package com.example.yumi.dto;

import java.util.List;

import lombok.Data;

@Data
public class CreateOrderRequest {

	private String userId;

	private List<ProductInfo> productInfo;
}
