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

## 環境需求

- **JDK 17 以上**：執行 `java -version` 確認版本，且 `JAVA_HOME` 需指向該 JDK
- **不需要另外安裝 Maven**：專案內附 Maven Wrapper（`mvnw`），第一次執行會自動下載
- **SQL Server**：只有啟動應用程式需要，執行測試不需要

以下指令都在**專案根目錄**（`pom.xml` 所在的資料夾）執行：

```bash
git clone https://github.com/Minnieyuu/MyECommerce.git
cd MyECommerce
```

| 系統 | 指令寫法 |
|---|---|
| macOS / Linux | `./mvnw ...` |
| Windows（cmd） | `mvnw ...` |
| Windows（PowerShell） | `.\mvnw ...` |

---

## 測試執行方式

```bash
./mvnw test
```

測試使用 H2 記憶體資料庫，clone 下來即可直接執行。看到 `BUILD SUCCESS` 即全部通過。

---

## 啟動方式

1. 在 SQL Server 建立資料庫（資料表會在啟動時自動建立）：

   ```sql
   CREATE DATABASE [my-ecommerce];
   ```

2. 修改 `src/main/resources/application.properties` 的連線位址、帳號、密碼

3. 啟動：

   ```bash
   ./mvnw spring-boot:run
   ```

4. 看到 `Started MyECommerceApplication` 即啟動成功，API 位址為 `http://localhost:8080`

**測試案例**（`src/test/java/com/example/yumi/OrderServiceTest.java`）

| 情境 | 怎麼測 | 驗證什麼 |
|---|---|---|
| 正常下單 | 一張訂單買兩種商品 | 兩種商品都扣到庫存；金額為 50000×2 + 8000×3 = 124000；狀態為 `CREATED` |
| 庫存不足 | 一個商品庫存夠、另一個不夠 | 回傳 `INSUFFICIENT_STOCK`；**所有庫存不變** |
| 商品不存在 | 買一個不存在的商品 id | 回傳 `PRODUCT_NOT_FOUND` |
| 商品未上架 | 買一個下架商品 | 回傳 `PRODUCT_OFF_SALE`；庫存不變 |
| 重複下單（內容相同） | 同一個 key 送兩次相同內容 | 同一筆 orderId；庫存只扣一次；資料庫只有一張訂單 |
| 重複下單（內容不同） | 同一個 key，第二次改購買數量 | 回傳 `IDEMPOTENCY_CONFLICT`；庫存只扣第一次的量 |
| 重複下單（同時送出） | 5 個執行緒用同一個 key 同時下單 | 只有一張訂單、庫存只扣一次；其餘收到 `IDEMPOTENCY_IN_PROGRESS`；用同一個 key 重試拿到那張訂單 |
| 併發搶購 | 20 個執行緒同時搶 10 個庫存 | 剛好 10 人成功、庫存歸零、10 張訂單；**失敗的人原因都是庫存不足** |
| 同時下單與補貨 | 10 人下單的同時，管理員補貨 +5 並改名稱 | 庫存 = 10 + 5 − 10 = 5；名稱也有更新，兩邊的修改都沒被蓋掉 |
| 取消訂單 | 買 3 個後取消 | 庫存加回；狀態為 `CANCELLED` |
| 重複取消 | 同一張訂單取消兩次 | 兩次都成功；庫存只加回一次 |

併發測試用 `CountDownLatch` 讓所有執行緒準備好後同時開始，確保真的發生搶購。實際把悲觀鎖拿掉重跑時，20 人會全部下單成功，證明是鎖在防止超賣。

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
| `IDEMPOTENCY_IN_PROGRESS` | 409 | 相同 key 的請求正在處理中，請以相同 key 重試 |
| `IDEMPOTENCY_CONFLICT` | 422 | 相同 key 但內容不同 |
| `INTERNAL_ERROR` | 500 | 系統錯誤 |

---

## AI 使用說明

開發過程使用 Claude 協助。做法是先和 AI 討論可行的方案，比較後選定做法，再請 AI 產出程式碼或審查我寫的程式。

**AI 產出的部分**：多數程式碼初版、測試寫法、README 初稿。

**過程中我做的決策**

- **商品更新的併發問題**：寫完更新 API 後，想到「管理員更新時剛好有人下單」的情況，發現會用舊資料蓋掉已扣的庫存。因此更新時也加鎖，並把庫存改為傳增減量。
- **錯誤處理方式的調整**：一開始選擇讓 Service 回傳結果碼，因為檢查都在寫入之前，失敗時沒有資料需要 rollback。後來商品 API 和訂單查詢的回傳物件放不下錯誤碼，為了讓所有錯誤格式一致，改為統一拋出 `BusinessException`，回應物件也隨之簡化成只放資料。
- **訂單編號**：想改用 `OD + 日期 + 流水號` 方便客服辨識，但流水號若用「查詢最大號再加 1」，同時下單時兩筆會拿到同一個號碼。因此改用資料庫自增 id 組出流水號，由資料庫保證唯一。
- **相同 Idempotency-Key 同時送達**：兩個請求會同時查不到 key、都去建單，第二筆撞到 unique key。曾考慮讓相同請求在商品鎖排隊，但若同一個 key 買的是不同商品就鎖不到同一把鎖。最後選擇不加鎖，直接接住 unique 衝突回 409，請前端用同一個 key 重試，重試時就會拿到同一筆 orderId。
- **付款、取消 API**：修正了重複取消會重複加回庫存等問題。
- **非本人的訂單回 404**：查詢、付款、取消時，「訂單不存在」和「不是本人的訂單」都回 404。若回 403，等於告訴對方這個訂單編號存在，可被用來猜測、探測別人的訂單。
- **取消訂單依狀態決定是否退款**：已付款的訂單先退款再取消，未付款的直接取消。退款失敗就不取消，避免出現「錢沒退、訂單卻已取消」的狀況。
