package com.example.yumi.Enum;

import lombok.Getter;

@Getter

public enum ResultCode {

	SUCCESS("200", "OK"),
	PRODUCT_OFF_SALE("400", "商品未上架"),
	PRODUCT_NOT_FOUND("404", "商品不存在"),
	INSUFFICIENT_STOCK("400", "庫存不足"),
	IDEMPOTENCY_CONFLICT("422", "Unprocessable Entity");

	private String code;
	private String message;

	private ResultCode(String code, String message) {
		this.code = code;
		this.message = message;
	}

}
