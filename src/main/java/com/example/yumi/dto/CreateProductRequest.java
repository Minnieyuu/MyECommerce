package com.example.yumi.dto;

import java.math.BigDecimal;

import com.example.yumi.Enum.ProductStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
		@NotBlank @Size(max = 200) String name,
		@NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal price,
		@NotNull @PositiveOrZero Integer stock,
		@NotNull ProductStatus status) {
}
