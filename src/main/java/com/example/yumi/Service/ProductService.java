package com.example.yumi.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.yumi.Dao.ProductDao;
import com.example.yumi.Enum.ResultCode;
import com.example.yumi.Exceptions.BusinessException;
import com.example.yumi.dto.CreateProductRequest;
import com.example.yumi.dto.ProductResponse;
import com.example.yumi.dto.UpdateProductRequest;
import com.example.yumi.entity.Product;

import jakarta.transaction.Transactional;

@Service
public class ProductService {

	@Autowired
	ProductDao productDao;

	@Transactional
	public ProductResponse create(CreateProductRequest request) {
		Product product = new Product(request.name().strip(), request.price(), request.stock(), request.status());
		return ProductResponse.from(productDao.save(product));
	}

	@Transactional
	public ProductResponse update(Long id, UpdateProductRequest request) {
		Product product = productDao.findByIdForUpdate(id)
				.orElseThrow(() -> new BusinessException(ResultCode.PRODUCT_NOT_FOUND));

		// 先檢查：調整後的庫存不能是負數
		if (request.stockChange() != null) {
			int newStock = product.getStock() + request.stockChange();
			if (newStock < 0) {
				throw new BusinessException(ResultCode.STOCK_BELOW_ZERO);
			}
		}

		// 不是null才update
		if (request.name() != null) {
			product.setName(request.name().strip());
		}
		if (request.price() != null) {
			product.setPrice(request.price());
		}
		if (request.stockChange() != null) {
			product.setStock(product.getStock() + request.stockChange());
		}
		if (request.status() != null) {
			product.setStatus(request.status());
		}
		return ProductResponse.from(product);
	}

}
