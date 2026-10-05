package com.example.yumi.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Collectors;

import com.example.yumi.dto.CreateOrderRequest;
import com.example.yumi.dto.ProductInfo;

public final class RequestFingerprint {

	public static String of(CreateOrderRequest req) {
		String items = req.getProductInfo().stream()
				.sorted(Comparator.comparing(ProductInfo::getProductId))
				.map(p -> p.getProductId() + ":" + p.getCount())
				.collect(Collectors.joining(","));
		String canonical = "userId=" + req.getUserId() + "|items=" + items;
		return sha256Hex(canonical);
	}

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