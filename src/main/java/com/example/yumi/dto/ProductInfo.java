package com.example.yumi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductInfo {

	@NotNull
	private Long productId;

	@NotNull
	@Min(1) // 不可小於1
	private Integer count;

}
