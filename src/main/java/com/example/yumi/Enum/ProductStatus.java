package com.example.yumi.Enum;

import lombok.Getter;

@Getter
public enum ProductStatus {

	ON_SALE("上架"), OFF_SALE("下架");

	private String desc;

	private ProductStatus(String desc) {
		this.desc = desc;
	}
}
