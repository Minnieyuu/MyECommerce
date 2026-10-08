package com.example.yumi.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import com.example.yumi.dto.CreateOrderRequest;
import com.example.yumi.dto.ProductInfo;

public final class RequestFingerprint {

	/**
	 * 將requestbody排序，方便後續轉成雜湊值
	 * 
	 * @param req
	 * @return
	 */
	public static String of(CreateOrderRequest req) {
		// 複製一份再排序，不動到原本的 request
		List<ProductInfo> sorted = new ArrayList<>(req.getProductInfo());
		sorted.sort(Comparator.comparing(ProductInfo::getProductId));

		// 組成 "商品id:數量,商品id:數量" 的格式，例如 "1:2,2:3"
		StringBuilder items = new StringBuilder();
		for (ProductInfo p : sorted) {
			if (items.length() > 0) {
				items.append(",");
			}
			items.append(p.getProductId()).append(":").append(p.getCount());
		}

		String canonical = "userId=" + req.getUserId() + "|items=" + items;
		return sha256Hex(canonical);
	}

	/**
	 * 轉成雜湊值，方便後續用來比對requestBody是否相同
	 * 
	 * @param s
	 * @return
	 */
	private static String sha256Hex(String s) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
					.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}