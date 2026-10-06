package com.example.yumi.Dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.yumi.entity.Product;

import jakarta.persistence.LockModeType;

public interface ProductDao extends JpaRepository<Product, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE) // 悲觀鎖鎖住，直到Transactional完成
	@Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
	List<Product> findAllByIdForUpdate(@Param("ids") List<Long> ids);

	@Lock(LockModeType.PESSIMISTIC_WRITE) // 鎖住直到Transactional完成
	@Query("SELECT p FROM Product p WHERE p.id = :id")
	Optional<Product> findByIdForUpdate(@Param("id") Long id);

}
