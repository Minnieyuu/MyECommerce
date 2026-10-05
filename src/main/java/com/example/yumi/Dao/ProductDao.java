package com.example.yumi.Dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.yumi.entity.Product;

import jakarta.persistence.LockModeType;

public interface ProductDao extends JpaRepository<Product, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE) // 樂觀鎖鎖住，直到Transactional完成
	@Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
	List<Product> findAllByIdForUpdate(@Param("ids") List<Long> ids);

}
