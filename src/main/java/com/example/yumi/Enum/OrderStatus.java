package com.example.yumi.Enum;

import lombok.Getter;

@Getter
public enum OrderStatus {

	CREATED("已建立"),
	PAID("已付款"),
	CANCELLED("已取消"),
	COMPLETE("已完成");

	private String desc;

	private OrderStatus(String desc) {
		this.desc = desc;
	}

}
