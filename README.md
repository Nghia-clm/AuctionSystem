**Mối quan hệ**

## Người A (Server core) → là nền tảng của tất cả

B phụ thuộc vào A vì dao/service/ cần dùng các class từ model/ của A (ví dụ: User, Item, Auction...)
C phụ thuộc vào A vì client giao tiếp qua network/ của A
D phụ thuộc vào A vì AutoBidder, AntiSniping cần gọi vào AuctionManager của A

→ Người A phải làm trước, vì B, C, D đều cần A xong mới làm được

## Người B (Data layer) → phục vụ cho A và C

B viết service/ để A có thể gọi logic nghiệp vụ
C gián tiếp dùng kết quả từ B thông qua server (dữ liệu hiển thị lên giao diện đều từ DB của B)

→ Người B làm song song với A, nhưng cần A định nghĩa model trước

## Người C (Client) → phụ thuộc vào A và B

C chỉ bắt đầu làm hiệu quả khi A đã có network/ và B đã có service/ chạy được
C không ảnh hưởng ngược lại A hay B

→ Người C nên làm sau, hoặc làm giao diện trước rồi kết nối sau

## Người D (Advanced + Test) → làm cuối cùng

Test cần A, B, C hoàn thiện mới test được
AutoBidder, AntiSniping cần server của A chạy ổn định trước

→ Người D làm cuối, nhưng có thể setup CI/CD và viết test case sớm

---

## 👥 Thành viên nhóm

| Họ tên | Phụ trách |
|--------|-----------|
| Người A | Server core: `model/`, `Main.java`, `network/`, `manager/` |
| Người B | Server data: `dao/`, `service/`, `factory/`, `exception/`, `database/` |
| Người C | Client: `controller/`, `network/`, `chart/`, toàn bộ `.fxml` |
| Người D | Advanced: `AutoBidder.java`, `AntiSnipingTimer.java`, `BidHistoryVisualizer.java`, `test/`, `pom.xml`, CI/CD |

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
mvn exec:java -'Dexec.mainClass="com.auction.Main'
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
├── .github/workflows/ci.yml     ← CI/CD GitHub Actions        ← Người D
├── pom.xml                      ← Maven config                ← Người D
├── database/
│   ├── scheme.sql               ← Tạo bảng MySQL              ← Người B
│   └── sample_data.sql          ← Dữ liệu mẫu                 ← Người B
├── sever/src/main/java/com/auction/
│   ├── Main.java                ← Khởi động Server            ← Người A
│   ├── model/                   ← Entity, User, Item, Auction  ← Người A
│   ├── dao/                     ← Truy cập database (JDBC)    ← Người B
│   ├── service/                 ← Logic nghiệp vụ             ← Người B
│   ├── network/                 ← Socket Server               ← Người A
│   ├── manager/                 ← AuctionManager (Singleton)  ← Người A
│   ├── factory/                 ← ItemFactory (Factory)       ← Người B
│   ├── observer/                ← BidObserver (Observer)      ← Người A
│   └── exception/               ← Custom Exceptions           ← Người B
├── client/src/main/java/com/auction/
│   ├── MainApp.java             ← Khởi động JavaFX Client     ← Người C
│   ├── controller/              ← JavaFX Controllers (MVC)    ← Người C
│   ├── network/                 ← ServerConnection            ← Người C
│   └── chart/                   ← BidHistoryChart             ← Người C
├── client/src/main/resources/com/auction/view/
│   ├── login.fxml                                             ← Người C
│   ├── auction_list.fxml                                      ← Người C
│   ├── bid_screen.fxml                                        ← Người C
│   └── seller_panel.fxml                                      ← Người C
├── advanced/src/main/java/
│   ├── AutoBidder.java          ← Đấu giá tự động             ← Người D
│   ├── AntiSnipingTimer.java    ← Gia hạn phiên               ← Người D
│   └── BidHistoryVisualizer.java ← Biểu đồ lịch sử giá       ← Người D
└── test/src/test/java/
    ├── AuctionTest.java                                       ← Người D
    ├── BidValidationTest.java                                 ← Người D
    ├── ItemFactoryTest.java                                   ← Người D
    └── UserServiceTest.java                                   ← Người D
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
