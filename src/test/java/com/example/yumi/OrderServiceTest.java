package com.example.yumi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.yumi.Dao.OrderDao;
import com.example.yumi.Dao.ProductDao;
import com.example.yumi.Enum.OrderStatus;
import com.example.yumi.Enum.ProductStatus;
import com.example.yumi.Enum.ResultCode;
import com.example.yumi.Exceptions.BusinessException;
import com.example.yumi.Service.OrderService;
import com.example.yumi.Service.ProductService;
import com.example.yumi.dto.CreateOrderRequest;
import com.example.yumi.dto.GetOrderDto;
import com.example.yumi.dto.OrderResponse;
import com.example.yumi.dto.ProductInfo;
import com.example.yumi.dto.ProductResponse;
import com.example.yumi.dto.UpdateProductRequest;
import com.example.yumi.entity.Order;
import com.example.yumi.entity.Product;

@SpringBootTest
public class OrderServiceTest {

	@Autowired
	OrderService orderService;

	@Autowired
	OrderDao orderDao;

	@Autowired
	ProductService productService;

	@Autowired
	ProductDao productDao;

	@AfterEach
	void cleanUp() {
		orderDao.deleteAll();
		productDao.deleteAll();
	}

	@Test
	void 正常下單_扣庫存並計算金額() {

		// Given：兩個上架商品
		Product iphone = productDao.save(
				new Product("iPhone 20", new BigDecimal("50000"), 100, ProductStatus.ON_SALE));
		Product airpods = productDao.save(
				new Product("AirPods", new BigDecimal("8000"), 50, ProductStatus.ON_SALE));

		// iphone*2 、airpods*3
		OrderResponse response = orderService.create(UUID.randomUUID(),
				order(item(iphone.getId(), 2), item(airpods.getId(), 3)));

		// Then：兩個商品都有扣庫存
		assertEquals(98, productDao.findById(iphone.getId()).get().getStock());
		assertEquals(47, productDao.findById(airpods.getId()).get().getStock());

		// Then：訂單有 2 筆明細，金額 50000×2 + 8000×3 = 124000
		Order order = orderDao.findWithItemsById(response.getOrderId()).get();
		assertEquals(2, order.getItems().size());
		assertEquals(0, new BigDecimal("124000").compareTo(order.getTotalAmount()));
		assertEquals(OrderStatus.CREATED, order.getStatus());
	}

	@Test
	void 庫存不足_回傳錯誤且不扣任何庫存() {

		// 庫存不足測試
		Product enough = productDao.save(
				new Product("庫存充足商品", new BigDecimal("50000"), 100, ProductStatus.ON_SALE));
		Product notEnough = productDao.save(
				new Product("庫存不足", new BigDecimal("8000"), 1, ProductStatus.ON_SALE));

		// When：
		BusinessException e = assertThrows(BusinessException.class,
				() -> orderService.create(UUID.randomUUID(),
						order(item(enough.getId(), 2), item(notEnough.getId(), 3))));

		// Then：回傳庫存不足
		assertEquals(ResultCode.INSUFFICIENT_STOCK, e.getResultCode());

		// Then：兩個商品的庫存都沒變，包括庫存充足的
		assertEquals(100, productDao.findById(enough.getId()).get().getStock());
		assertEquals(1, productDao.findById(notEnough.getId()).get().getStock());
	}

	@Test
	void 商品不存在_回傳錯誤() {
		// When：買一個不存在的商品
		BusinessException e = assertThrows(BusinessException.class,
				() -> orderService.create(UUID.randomUUID(), order(item(999999L, 1))));

		// Then
		assertEquals(ResultCode.PRODUCT_NOT_FOUND, e.getResultCode());
	}

	@Test
	void 商品未上架_回傳錯誤且不扣庫存() {
		// Given：一個下架商品
		Product product = productDao.save(
				new Product("舊款手機", new BigDecimal("10000"), 10, ProductStatus.OFF_SALE));

		// When
		BusinessException e = assertThrows(BusinessException.class,
				() -> orderService.create(UUID.randomUUID(), order(item(product.getId(), 1))));

		// Then
		assertEquals(ResultCode.PRODUCT_OFF_SALE, e.getResultCode());
		assertEquals(10, productDao.findById(product.getId()).get().getStock());
	}

	@Test
	void 重複下單_相同key相同內容_回傳同一筆訂單且只扣一次庫存() {
		// Given
		Product product = productDao.save(
				new Product("iPhone 20", new BigDecimal("50000"), 100, ProductStatus.ON_SALE));
		UUID key = UUID.randomUUID();

		// When：同一個 key，送兩次一模一樣的內容
		OrderResponse first = orderService.create(key, order(item(product.getId(), 2)));
		OrderResponse second = orderService.create(key, order(item(product.getId(), 2)));

		// Then：兩次是同一筆 orderId
		assertEquals(first.getOrderId(), second.getOrderId());

		// Then：庫存只扣一次（100 - 2 = 98，不是 96）
		assertEquals(98, productDao.findById(product.getId()).get().getStock());

		// Then：資料庫裡只有一張訂單
		assertEquals(1, orderDao.count());
	}

	@Test
	void 重複下單_相同key不同RequestBody_回傳422且不扣庫存() {
		// Given
		Product product = productDao.save(
				new Product("iPhone 20", new BigDecimal("50000"), 100, ProductStatus.ON_SALE));
		UUID key = UUID.randomUUID();

		// When：同一個 key，第一次買 2 個，第二次改成買 5 個
		orderService.create(key, order(item(product.getId(), 2)));

		// 檢查有沒有丟出正確exception
		BusinessException e = assertThrows(BusinessException.class,
				() -> orderService.create(key, order(item(product.getId(), 5))));

		// Then：第二次回傳 422
		assertEquals(ResultCode.IDEMPOTENCY_CONFLICT, e.getResultCode());

		// Then：庫存只有扣掉第一次買的 2 個
		assertEquals(98, productDao.findById(product.getId()).get().getStock());
	}

	@Test
	void 二十人同時搶十個庫存_剛好賣出十個_不會超賣() throws Exception {

		// 設定庫存 10 個
		final Integer stock = 10;

		// Given
		Product product = productDao.save(
				new Product("限量商品", new BigDecimal("1000"), stock, ProductStatus.ON_SALE));

		int people = 20;
		ExecutorService pool = Executors.newFixedThreadPool(people); // 準備 20 個執行緒
		CountDownLatch startGun = new CountDownLatch(1); // 起跑槍

		// When：20人搶，每人固定搶一個
		List<Future<OrderResponse>> futures = new ArrayList<>();
		for (int i = 0; i < people; i++) {

			// 建立任務：等countDown
			Callable<OrderResponse> task = new Callable<OrderResponse>() {
				@Override
				public OrderResponse call() throws Exception {
					startGun.await();
					return orderService.create(UUID.randomUUID(), order(item(product.getId(), 1)));
				}
			};

			// 提交任務到執行緒池，取得 Future 以便之後拿回結果
			Future<OrderResponse> future = pool.submit(task);

			futures.add(future);
		}

		startGun.countDown(); // 20個執行緒同時執行

		// 收回每個人的結果，計算成功人數
		int successCount = 0;

		for (Future<OrderResponse> f : futures) {
			try {
				f.get(); // 拿回結果，沒拋例外就是成功
				successCount++;
			} catch (ExecutionException e) {
				// 沒搶到的人：必須是庫存不足，不能是其他錯誤
				BusinessException be = assertInstanceOf(BusinessException.class, e.getCause());
				assertEquals(ResultCode.INSUFFICIENT_STOCK, be.getResultCode());
			}
		}

		// 執行緒關閉
		pool.shutdown();

		// Then：確認成功人數是否與庫存相符合
		assertEquals(stock, successCount);

		// Then：庫存剛好歸零，沒有變成負數
		assertEquals(0, productDao.findById(product.getId()).get().getStock());

		// Then：剛好 10 張訂單
		assertEquals(10, orderDao.count());
	}

	@Test
	void 取消訂單_庫存加回且狀態變已取消() {
		// Given：庫存 10，買 3 個後剩 7
		Product product = productDao.save(
				new Product("iPhone 20", new BigDecimal("50000"), 10, ProductStatus.ON_SALE));
		OrderResponse created = orderService.create(UUID.randomUUID(), order(item(product.getId(), 3)));
		String orderId = created.getOrderId();

		// When：取消訂單
		orderService.cancel("user1", orderId);

		// Then：庫存加回 10
		assertEquals(10, productDao.findById(product.getId()).get().getStock());

		// Then：訂單狀態變成已取消
		assertEquals(OrderStatus.CANCELLED, orderDao.findWithItemsById(orderId).get().getStatus());
	}

	@Test
	void 正常取消_重複取消_庫存不會重複加回() {

		Integer stock = 10;

		String userId = "user1";

		// 新增商品
		Product product = productDao.save(
				new Product("iPhone 20", new BigDecimal("50000"), stock, ProductStatus.ON_SALE));

		// 新增訂單
		OrderResponse created = orderService.create(UUID.randomUUID(), order(item(product.getId(), 3)));

		// 取得訂單編號
		String orderId = created.getOrderId();

		// 第一次取消
		orderService.cancel(userId, orderId);

		// 第一次取消，查詢訂單
		GetOrderDto orderFirst = orderService.getOrder(userId, orderId);

		// 第一次取消，檢查訂單狀態
		assertEquals(OrderStatus.CANCELLED, orderFirst.getStatus());

		// 檢查第一次取消後，庫存是否正確
		assertEquals(stock, productDao.findById(product.getId()).get().getStock());

		// 第二次取消：不會拋例外，視為成功
		orderService.cancel(userId, orderId);

		// 第二次取消，查詢訂單
		GetOrderDto orderSecond = orderService.getOrder(userId, orderId);

		// 第二次取消，檢查訂單狀態
		assertEquals(OrderStatus.CANCELLED, orderSecond.getStatus());

		// 檢查第二次取消後，庫存是否正確
		assertEquals(stock, productDao.findById(product.getId()).get().getStock());
	}

	@Test
	void 同時下單與補貨_庫存數字正確() throws Exception {

		// Given：庫存 10
		Product product = productDao.save(
				new Product("iPhone 20", new BigDecimal("50000"), 10, ProductStatus.ON_SALE));

		ExecutorService pool = Executors.newFixedThreadPool(11); // 10 個買家 + 1 個管理員

		CountDownLatch startGun = new CountDownLatch(1);

		// 10 個買家：各買 1 個
		List<Future<OrderResponse>> buyers = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			Callable<OrderResponse> buyTask = new Callable<OrderResponse>() {
				@Override
				public OrderResponse call() throws Exception {
					startGun.await();
					return orderService.create(UUID.randomUUID(), order(item(product.getId(), 1)));
				}
			};
			buyers.add(pool.submit(buyTask));
		}

		// 1 個管理員：補貨 +5，同時改名稱
		Callable<ProductResponse> restockTask = new Callable<ProductResponse>() {
			@Override
			public ProductResponse call() throws Exception {
				startGun.await();
				return productService.update(product.getId(),
						new UpdateProductRequest("iPhone 20 新版", null, 5, null));
			}
		};
		Future<ProductResponse> admin = pool.submit(restockTask);

		startGun.countDown(); // 開放所有執行緒

		// 等待所有任務完成，計算成功人數（失敗的話 get() 會拋例外，測試直接失敗）
		int successCount = 0;

		for (Future<OrderResponse> f : buyers) {
			f.get();
			successCount++;
		}

		// 用於確保賣家成功捕貨
		admin.get();

		// 執行緒關閉
		pool.shutdown();

		// Then：10 個人都買到
		assertEquals(10, successCount);

		Product after = productDao.findById(product.getId()).get();

		// Then:檢查庫存數量是否正確
		assertEquals(5, after.getStock());

		// 檢查商品名稱是否成功修改
		assertEquals("iPhone 20 新版", after.getName());
	}

	// 小工具：一個商品項目
	private ProductInfo item(Long productId, int count) {
		ProductInfo item = new ProductInfo();
		item.setProductId(productId);
		item.setCount(count);
		return item;
	}

	// 小工具：一張訂單，可以放任意多個商品
	private CreateOrderRequest order(ProductInfo... items) {
		CreateOrderRequest req = new CreateOrderRequest();
		req.setUserId("user1");
		req.setProductInfo(List.of(items));
		return req;
	}

}
