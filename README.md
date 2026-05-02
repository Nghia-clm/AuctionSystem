## 👥 Thành viên nhóm

| Họ tên | Vai trò |
|--------|---------|
| Người A | Server: Model, Network, Observer, AuctionManager |
| Người B | Server: DAO, Service, Factory, Exception, Unit Test |
| Người C | Client: JavaFX Controller, FXML, ServerConnection |
| Người D | Advanced: AutoBidder, AntiSniping, BidHistoryChart, Maven |

---

## 🏗️ Kiến trúc hệ thống

```
CLIENT (JavaFX + MVC)  ◄──── JSON/Socket Port 9999 ────►  SERVER (Java)
                                                                  │
                                                           MySQL Database
                                                            auction_db
```

### Các tầng phía Server
- **Model** – Entity, User (Bidder/Seller/Admin), Item (Electronics/ArtItem/Vehicle), Auction, BidTransaction
- **DAO** – UserDAO, ItemDAO, AuctionDAO, BidTransactionDAO (JDBC + MySQL)
- **Service** – UserService, AuctionService, ItemService
- **Network** – ServerMain (thread pool 50), ClientHandler (xử lý request JSON)
- **Manager** – AuctionManager (Singleton, broadcast realtime)

### Các tầng phía Client
- **Controller** – LoginController, AuctionListController, BidController, SellerController
- **View** – login.fxml, auction_list.fxml, bid_screen.fxml, seller_panel.fxml
- **Network** – ServerConnection (Singleton)
- **Chart** – BidHistoryChart (JavaFX LineChart, realtime)

---

## 🎯 Design Patterns áp dụng

| Pattern | Áp dụng ở đâu |
|---------|--------------|
| **Singleton** | `DatabaseConnection`, `AuctionManager`, `ServerConnection` |
| **Factory Method** | `ItemFactory` – tạo Electronics / ArtItem / Vehicle theo type |
| **Observer** | `BidObserver` – broadcast giá mới tới toàn bộ client đang xem phiên |

---

## ✅ Chức năng đã hoàn thiện

### Bắt buộc
- [x] Đăng ký / đăng nhập với 3 vai trò: Bidder, Seller, Admin
- [x] Quản lý sản phẩm: thêm / sửa / xóa (Seller)
- [x] Tạo và quản lý phiên đấu giá
- [x] Đặt giá realtime, kiểm tra hợp lệ
- [x] Tự động đóng phiên khi hết giờ
- [x] Chuyển trạng thái: OPEN → RUNNING → FINISHED → PAID/CANCELED
- [x] Xử lý ngoại lệ: AuctionClosedException, InvalidBidException, UserNotFoundException
- [x] Giao diện JavaFX đầy đủ 4 màn hình
- [x] Concurrent Bidding an toàn (ReentrantReadWriteLock)
- [x] Realtime update qua Socket broadcast (không polling)
- [x] Unit Test JUnit cho logic quan trọng
- [x] CI/CD GitHub Actions tự động chạy test khi push

### Nâng cao (bonus)
- [x] **Auto-Bidding** – đặt maxBid + increment, hệ thống tự đấu giá thay
- [x] **Anti-sniping** – tự động gia hạn phiên nếu có bid trong X giây cuối
- [x] **Bid History Visualization** – biểu đồ đường giá realtime (JavaFX LineChart)

---

## 🛠️ Yêu cầu môi trường

| Công cụ | Phiên bản |
|---------|-----------|
| Java | 17+ |
| Maven | 3.8+ |
| MySQL | 8.x |
| JavaFX | 21 (tự động tải qua Maven) |

---

## ⚙️ Cài đặt và chạy

### Bước 1 – Clone repo
```bash
git clone https://github.com/<your-username>/AuctionSystem.git
cd AuctionSystem
```

### Bước 2 – Tạo database MySQL
Mở MySQL Workbench, chạy lần lượt 2 file:
```
database/scheme.sql       ← tạo bảng
database/sample_data.sql  ← thêm dữ liệu mẫu
```

### Bước 3 – Sửa thông tin kết nối database
Mở file `sever/src/main/java/com/auction/dao/DatabaseConnection.java`, sửa:
```java
private static final String PASSWORD = "your_password"; // đổi thành password MySQL của bạn
```

### Bước 4 – Build project
```bash
mvn compile
```

### Bước 5 – Chạy Server (Terminal 1)
```bash
mvn exec:java '-Dexec.mainClass=com.auction.Main'
```
Chờ thấy dòng: `=== Auction Server started on port 9999 ===`

### Bước 6 – Chạy Client (Terminal 2)
```bash
mvn javafx:run
```

---

## 🔑 Tài khoản mẫu

| Username | Password | Role |
|----------|----------|------|
| `admin` | `password123` | ADMIN |
| `seller_alice` | `password123` | SELLER |
| `bidder_charlie` | `password123` | BIDDER |

---

## 🧪 Chạy Unit Test

```bash
mvn test
```

Kết quả mong đợi:
```
Tests run: X, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 📁 Cấu trúc thư mục

```
AuctionSystem/
├── .github/workflows/ci.yml     ← CI/CD GitHub Actions
├── pom.xml                      ← Maven config
├── database/
│   ├── scheme.sql               ← Tạo bảng MySQL
│   └── sample_data.sql          ← Dữ liệu mẫu
├── server/src/main/java/com/auction/
│   ├── Main.java                ← Khởi động Server
│   ├── model/                   ← Entity, User, Item, Auction...
│   ├── dao/                     ← Truy cập database (JDBC)
│   ├── service/                 ← Logic nghiệp vụ
│   ├── network/                 ← Socket Server
│   ├── manager/                 ← AuctionManager (Singleton)
│   ├── factory/                 ← ItemFactory (Factory Pattern)
│   ├── observer/                ← BidObserver (Observer Pattern)
│   └── exception/               ← Custom Exceptions
├── client/src/main/java/com/auction/
│   ├── MainApp.java             ← Khởi động JavaFX Client
│   ├── controller/              ← JavaFX Controllers (MVC)
│   ├── network/                 ← ServerConnection
│   └── chart/                   ← BidHistoryChart
├── client/src/main/resources/com/auction/view/
│   ├── login.fxml
│   ├── auction_list.fxml
│   ├── bid_screen.fxml
│   └── seller_panel.fxml
├── advanced/src/main/java/
│   ├── AutoBidder.java          ← Đấu giá tự động
│   ├── AntiSnipingTimer.java    ← Gia hạn phiên
│   └── BidHistoryVisualizer.java
└── test/src/test/java/
    ├── AuctionTest.java
    ├── BidValidationTest.java
    ├── ItemFactoryTest.java
    └── UserServiceTest.java
```

---

## 📡 Giao thức Client–Server

Giao tiếp qua **JSON over TCP Socket**, mỗi message là 1 dòng.

**Request:**
```json
{ "action": "LOGIN", "data": { "username": "alice", "password": "123456" } }
```

**Response:**
```json
{ "status": "OK",    "data": { ... }, "message": "Đăng nhập thành công" }
{ "status": "ERROR", "message": "Tên đăng nhập hoặc mật khẩu không đúng" }
```

**Broadcast realtime (server → tất cả client trong phòng):**
```json
{ "event": "NEW_BID",          "auctionId": "...", "bidAmount": 500000 }
{ "event": "AUCTION_FINISHED", "auctionId": "...", "winnerId": "...", "finalPrice": 700000 }
{ "event": "AUCTION_EXTENDED", "auctionId": "...", "newEndTime": "...", "extraSeconds": 60 }
```
