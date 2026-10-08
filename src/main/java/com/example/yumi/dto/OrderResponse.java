package com.example.yumi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 成功時的回應；失敗時改由 GlobalExceptionHandler 回傳 ErrorResponse */
@Data
@AllArgsConstructor
public class OrderResponse {

	private String orderId;

}
