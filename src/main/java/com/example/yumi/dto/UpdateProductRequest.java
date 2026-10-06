package com.example.yumi.dto;

import java.math.BigDecimal;

import com.example.yumi.Enum.ProductStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** PATCH 欄位為 null 代表不更新。 */
public record UpdateProductRequest(
		@Size(min = 1, max = 200) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
		@Positive @Digits(integer = 10, fraction = 2) BigDecimal price,
		Integer stockChange,
		ProductStatus status) {
}