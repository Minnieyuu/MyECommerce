package com.example.yumi.Service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service

public class MockPaymentService {

	/**
	 * 模擬第三方金流付款，一律成功
	 */
	public boolean pay(String orderId, BigDecimal amount) {
		log.info("模擬付款 orderId={}, amount={}", orderId, amount);
		return true;
	}

	/**
	 * 模擬退款
	 * 
	 * @param orderId
	 * @param amount
	 * @return
	 */
	public boolean refund(String orderId, BigDecimal amount) {
		log.info("模擬退款 orderId={}, amount={}", orderId, amount);
		return true;
	}
}
