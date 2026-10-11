# MyECommerce：電商商品訂購系統

提供商品管理、建立訂單、訂單查詢，以及模擬付款與取消訂單。
設計決策請見 [DESIGN.md](DESIGN.md)。

**技術**：Java 17、Spring Boot 4、Spring Data JPA、SQL Server（測試用 H2）、Maven

---

## 系統架構

```
Controller → Service → Dao (JPA) → SQL Server
                 │
     錯誤一律拋出 BusinessException
                 ↓
GlobalExceptionHandler → 統一回傳 { code, message }
```

- **Controller**：接收請求、驗證輸入
- **Service**：庫存檢查與扣減、建立訂單、付款、取消
- **Dao**：會修改庫存或訂單狀態的查詢加上悲觀鎖，避免超賣

---

## 啟動方式


1. 手動建立資料庫：`CREATE DATABASE [my-ecommerce];`
2. 修改 `src/main/resources/application.properties` 的帳號密碼
3. 執行 `./mvnw spring-boot:run`（Windows：`.\mvnw spring-boot:run`）

---

## 測試執行方式

```bash
./mvnw test        # Windows：.\mvnw test
```

測試使用 H2 記憶體資料庫，**不需要安裝 SQL Server**。

> 若 `JAVA_HOME` 指向 Java 8 會編譯失敗，請改指向 JDK 17 以上。

**測試案例**
- 正常下單（多個商品）、庫存不足、商品不存在、商品未上架
- 重複下單：相同 key 相同內容、相同 key 不同內容
- 併發：20 人同時搶 10 個庫存，不超賣
- 併發：同時下單與補貨，庫存數字正確
- 取消訂單、重複取消不會重複加回庫存

---

## API

| Method | Path | 說明 | Header |
|---|---|---|---|
| POST | `/products` | 建立商品 | |
| PATCH | `/products/{id}` | 更新商品（含上下架） | |
| POST | `/orders` | 建立訂單 | `Idempotency-Key` |
| GET | `/orders/{orderId}` | 查詢訂單 | `userId` |
| POST | `/orders/{orderId}/pay` | 付款（模擬） | `userId` |
| POST | `/orders/{orderId}/cancel` | 取消訂單，庫存加回 | `userId` |

### 範例：建立訂單

```
POST /orders
Idempotency-Key: 3f2a8c1e-0b6d-4e7a-9c55-1d2e3f4a5b6c

{
  "userId": "user1",
  "productInfo": [
    { "productId": 1, "count": 2 },
    { "productId": 2, "count": 3 }
  ]
}
```

成功 `200`：

```json
{ "orderId": "OD20261008000001" }
```

### 更新商品的庫存

更新商品時傳的是**增減量** `stockChange`（例如補貨 `40`、盤損 `-3`），而不是直接設定庫存數字，避免蓋掉期間賣出的數量。

### 錯誤格式

```json
{ "code": "INSUFFICIENT_STOCK", "message": "庫存不足" }
```

| code | HTTP | 說明 |
|---|---|---|
| `INVALID_REQUEST` | 400 | 輸入格式錯誤 |
| `MISSING_HEADER` | 400 | 缺少必要 header |
| `PRODUCT_OFF_SALE` | 400 | 商品未上架 |
| `INSUFFICIENT_STOCK` | 400 | 庫存不足 |
| `DUPLICATE_PRODUCT` | 400 | 訂單內商品重複 |
| `STOCK_BELOW_ZERO` | 400 | 調整後庫存小於 0 |
| `ORDER_CANCEL_FAIL` | 400 | 取消失敗（退款失敗） |
| `PAYMENT_FAILED` | 402 | 付款失敗 |
| `PRODUCT_NOT_FOUND` | 404 | 商品不存在 |
| `ORDER_NOT_FOUND` | 404 | 訂單不存在或非本人訂單 |
| `ORDER_NOT_PAYABLE` | 409 | 訂單狀態不能付款 |
| `IDEMPOTENCY_CONFLICT` | 422 | 相同 key 但內容不同 |
| `INTERNAL_ERROR` | 500 | 系統錯誤 |

---

## 已知限制

相同 `Idempotency-Key` 的請求**同時**送達時，第二筆會收到 500（資料本身不會錯）。修正方式見 [DESIGN.md](DESIGN.md)。

---

## AI 使用說明

開發過程使用 Claude 協助。做法是先和 AI 討論可行的方案，比較後選定做法，再請 AI 產出程式碼或審查我寫的程式。

**AI 產出的部分**：多數程式碼初版、測試寫法、README 初稿。

**過程中我做的決策**

- **商品更新的併發問題**：寫完更新 API 後，想到「管理員更新時剛好有人下單」的情況，發現會用舊資料蓋掉已扣的庫存。因此更新時也加鎖，並把庫存改為傳增減量。
- **錯誤處理方式的調整**：一開始選擇讓 Service 回傳結果碼，因為檢查都在寫入之前，失敗時沒有資料需要 rollback。後來商品 API 和訂單查詢的回傳物件放不下錯誤碼，為了讓所有錯誤格式一致，改為統一拋出 `BusinessException`，回應物件也隨之簡化成只放資料。
- **訂單編號**：想改用 `OD + 日期 + 流水號` 方便客服辨識，但流水號若用「查詢最大號再加 1」，同時下單時兩筆會拿到同一個號碼。因此改用資料庫自增 id 組出流水號，由資料庫保證唯一。
- **相同 Idempotency-Key 同時送達**：確認問題存在後，決定先以 unique key 擋住。要觸發這個情況，同一個 key 必須在第一筆 commit 前的極短時間內重送，發生機率低；且資料正確性已由 unique key 保證，不會重複建單或扣庫存，影響只在回應碼。考量時間，列為已知限制，優先完成其他需求。
- **付款、取消 API**：修正了重複取消會重複加回庫存等問題。
- **非本人的訂單回 404**：查詢、付款、取消時，「訂單不存在」和「不是本人的訂單」都回 404。若回 403，等於告訴對方這個訂單編號存在，可被用來猜測、探測別人的訂單。
- **取消訂單依狀態決定是否退款**：已付款的訂單先退款再取消，未付款的直接取消。退款失敗就不取消，避免出現「錢沒退、訂單卻已取消」的狀況。
