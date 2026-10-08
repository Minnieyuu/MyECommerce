package com.example.yumi.Enum;

import lombok.Getter;

@Getter
public enum ResultCode {

	SUCCESS(200, "OK"),
	PRODUCT_OFF_SALE(400, "商品未上架"),
	PRODUCT_NOT_FOUND(404, "商品不存在"),
	ORDER_NOT_FOUND(404, "訂單不存在"),
	DUPLICATE_PRODUCT(400, "訂單內商品重複"),
	ORDER_NOT_PAYABLE(409, "訂單狀態無法付款"),
	PAYMENT_SUCCESS(200, "付款成功"),
	PAYMENT_FAILED(402, "付款失敗"),
	ORDER_CANCEL_SUCCESS(200, "訂單取消成功"),
	ORDER_CANCEL_FAIL(400, "訂單取消失敗"),
	INSUFFICIENT_STOCK(400, "庫存不足"),
	INVALID_REQUEST(400, "請求參數錯誤"),
	MISSING_HEADER(400, "缺少必要的 header"),
	API_NOT_FOUND(404, "API 不存在"),
	METHOD_NOT_ALLOWED(405, "不支援的 HTTP 方法"),
	INTERNAL_ERROR(500, "系統錯誤，請稍後再試"),
	IDEMPOTENCY_CONFLICT(422, "Unprocessable Entity");

	private int httpStatus;
	private String message;

	private ResultCode(int httpStatus, String message) {
		this.httpStatus = httpStatus;
		this.message = message;
	}

}
