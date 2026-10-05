package com.example.yumi.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ProductInfo {

	private Long productId;

	@Min(1) // 不可小於1
	private Integer count;

}
