package com.example.yumi.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CreateOrderRequest {

	@NotBlank
	private String userId;

	@NotEmpty // 至少要買一個商品
	@Valid // 往下驗證每個 ProductInfo
	private List<ProductInfo> productInfo;
}
