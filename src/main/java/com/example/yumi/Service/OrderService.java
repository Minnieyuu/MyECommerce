package com.example.yumi.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.yumi.Dao.OrderDao;
import com.example.yumi.Dao.ProductDao;
import com.example.yumi.Enum.OrderStatus;
import com.example.yumi.Enum.ProductStatus;
import com.example.yumi.Enum.ResultCode;
import com.example.yumi.Util.RequestFingerprint;
import com.example.yumi.dto.CreateOrderRequest;
import com.example.yumi.dto.GetOrderDto;
import com.example.yumi.dto.OrderResponse;
import com.example.yumi.dto.ProductInfo;
import com.example.yumi.entity.Order;
import com.example.yumi.entity.OrderItem;
import com.example.yumi.entity.Product;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Service
public class OrderService {

	@Autowired
	OrderDao orderDao;

	@Autowired
	ProductDao productDao;

	@Autowired
	MockPaymentService mockPaymentService;

	/**
	 * 檢查 1.重複下單 2.商品是否存在 3.檢查庫存 4.商品是否上架 最後 5.扣庫存、新增訂單(要同時完成，發生exception會rollback)
	 * 
	 * @param key
	 * @param req
	 * @return
	 */
	@Transactional
	public OrderResponse create(UUID key, CreateOrderRequest req) {
		String hash = RequestFingerprint.of(req);

		Optional<Order> existing = orderDao.findByIdempotencyKey(key);

		if (existing.isPresent()) {
			Order order = existing.get();

			// 相同 key + 不同 body → 422
			if (!order.getRequestHash().equals(hash)) {
				return new OrderResponse(ResultCode.IDEMPOTENCY_CONFLICT.name(),
						ResultCode.IDEMPOTENCY_CONFLICT.getMessage());
			}

			// 相同 key + 相同 body → 回傳同一筆 orderId
			return new OrderResponse(ResultCode.SUCCESS.name(),
					order.getOrderId());
		}

		List<Long> ids = new ArrayList<>();
		for (ProductInfo item : req.getProductInfo()) {

			// 同一個商品只能出現一次
			if (ids.contains(item.getProductId())) {
				return new OrderResponse(ResultCode.DUPLICATE_PRODUCT.name(),
						ResultCode.DUPLICATE_PRODUCT.getMessage());
			}

			ids.add(item.getProductId());
		}

		// 2. 查詢並鎖住這些商品
		List<Product> products = productDao.findAllByIdForUpdate(ids);

		// 3. 逐一檢查(庫存&商品是否存在&商品是否上架)
		for (ProductInfo item : req.getProductInfo()) {
			Product product = findProduct(products, item.getProductId());

			// 檢查商品是否存在
			if (product == null) {
				return new OrderResponse(ResultCode.PRODUCT_NOT_FOUND.name(),
						ResultCode.PRODUCT_NOT_FOUND.getMessage());
			}
			// 檢查庫存
			if (product.getStock() < item.getCount()) {
				return new OrderResponse(ResultCode.INSUFFICIENT_STOCK.name(),
						ResultCode.INSUFFICIENT_STOCK.getMessage());
			}

			// 檢查商品是否上架
			if (product.getStatus() != ProductStatus.ON_SALE) {
				return new OrderResponse(ResultCode.PRODUCT_OFF_SALE.name(),
						ResultCode.PRODUCT_OFF_SALE.getMessage());
			}
		}

		// 4. 全部檢查通過，開始扣庫存、建立訂單
		Order order = new Order();
		order.setUserId(req.getUserId());
		order.setIdempotencyKey(key);
		order.setRequestHash(hash);
		order.setCreatedAt(Instant.now());

		BigDecimal total = BigDecimal.ZERO;

		for (ProductInfo item : req.getProductInfo()) {
			Product product = findProduct(products, item.getProductId());

			// 扣庫存
			product.setStock(product.getStock() - item.getCount());

			// 建立訂單明細
			OrderItem orderItem = new OrderItem();
			orderItem.setOrder(order);
			orderItem.setProductId(product.getId());
			orderItem.setProductName(product.getName());
			orderItem.setUnitPrice(product.getPrice());
			orderItem.setQuantity(item.getCount());
			order.getItems().add(orderItem);

			// 累加金額：單價 × 數量
			total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getCount())));
		}

		order.setTotalAmount(total);

		// 5. 一起存檔
		productDao.saveAll(products);
		orderDao.save(order);

		// 產生訂單編號
		String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // 20261006
		order.setOrderId("OD" + date + String.format("%06d", order.getId()));

		return new OrderResponse(ResultCode.SUCCESS.name(), order.getOrderId());
	}

	private Product findProduct(List<Product> products, Long productId) {

		for (Product p : products) {
			if (p.getId().equals(productId)) {
				return p;
			}
		}
		return null;
	}

	@Transactional
	public GetOrderDto getOrder(String userId, String orderId) {

		Optional<Order> order = orderDao.findWithItemsById(orderId);

		// 查無訂單或非本人訂單 回傳空物件
		if (order.isEmpty() || !order.get().getUserId().equals(userId)) {

			return new GetOrderDto();

		}

		return GetOrderDto.parse(order.get());

	}

	@Transactional
	public OrderResponse pay(@NotBlank @Size(max = 64) String userId, String orderId) {

		Optional<Order> order = orderDao.findByOrderIdForUpdate(orderId);

		// 查無訂單或非本人訂單，回傳404
		if (order.isEmpty() || !order.get().getUserId().equals(userId)) {

			return new OrderResponse(ResultCode.ORDER_NOT_FOUND.name(),
					ResultCode.ORDER_NOT_FOUND.getMessage());

			// 訂單狀態除了CREATE之外，都不回傳409-訂單無法付款
		} else if (order.get().getStatus() != OrderStatus.CREATED) {
			return new OrderResponse(ResultCode.ORDER_NOT_PAYABLE.name(),
					ResultCode.ORDER_NOT_PAYABLE.getMessage());
		}

		// 呼叫模擬金流
		boolean isPaid = mockPaymentService.pay(orderId, order.get().getTotalAmount());

		if (!isPaid) {
			return new OrderResponse(ResultCode.PAYMENT_FAILED.name(),
					ResultCode.PAYMENT_FAILED.getMessage());
		}

		// 變更付款狀態
		order.get().setStatus(OrderStatus.PAID);

		return new OrderResponse(ResultCode.PAYMENT_SUCCESS.name(),
				ResultCode.PAYMENT_SUCCESS.getMessage());
	}

	@Transactional
	public OrderResponse cancel(@NotBlank @Size(max = 64) String userId, String orderId) {

		Optional<Order> order = orderDao.findByOrderIdForUpdate(orderId);

		// 查無訂單或非本人訂單，回傳404
		if (order.isEmpty() || !order.get().getUserId().equals(userId)) {

			return new OrderResponse(ResultCode.ORDER_NOT_FOUND.name(),
					ResultCode.ORDER_NOT_FOUND.getMessage());

		}

		// 已取消：直接回傳成功，不重複還庫存
		if (order.get().getStatus() == OrderStatus.CANCELLED) {
			return new OrderResponse(ResultCode.ORDER_CANCEL_SUCCESS.name(),
					ResultCode.ORDER_CANCEL_SUCCESS.getMessage());
		}

		// 已付款：先退款，退款失敗就不取消
		if (order.get().getStatus() == OrderStatus.PAID) {

			boolean isRefund = mockPaymentService.refund(orderId, order.get().getTotalAmount());

			if (!isRefund) {
				return new OrderResponse(ResultCode.ORDER_CANCEL_FAIL.name(),
						ResultCode.ORDER_CANCEL_FAIL.getMessage());
			}
		}

		// 還庫存：先鎖住商品再加回去
		List<Long> ids = new ArrayList<>();
		for (OrderItem item : order.get().getItems()) {
			ids.add(item.getProductId());
		}
		List<Product> products = productDao.findAllByIdForUpdate(ids);

		for (OrderItem item : order.get().getItems()) {
			Product product = findProduct(products, item.getProductId());
			product.setStock(product.getStock() + item.getQuantity());
		}

		order.get().setStatus(OrderStatus.CANCELLED);

		return new OrderResponse(ResultCode.ORDER_CANCEL_SUCCESS.name(),
				ResultCode.ORDER_CANCEL_SUCCESS.getMessage());
	}

}
