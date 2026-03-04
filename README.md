# Milk Tea Order Bot - Telegram

Bot Telegram tự động nhận đơn đặt hàng trà sữa, giúp chủ quán không cần phải trả lời từng tin nhắn của khách.

## Tính năng

- **Xem menu** theo danh mục: Trà Sữa, Trà Trái Cây, Cà Phê, Đá Xay
- **Chọn size** M / L cho mỗi món
- **Thêm topping** tùy chọn (chọn nhiều, toggle on/off)
- **Giỏ hàng** - xem, thêm, xoá từng món hoặc xoá tất cả
- **Đặt hàng** - xác nhận đơn, nhập tên + SĐT
- **Thông báo cho chủ quán** - bot tự động gửi thông tin đơn hàng đầy đủ
- **Đa người dùng** - nhiều khách đặt đồng thời, mỗi người có session riêng

## Yêu cầu

- Java 17+
- Maven 3.8+
- Telegram Bot Token (tạo qua [@BotFather](https://t.me/BotFather))

## Cài đặt & Chạy

### 1. Tạo Telegram Bot

1. Mở Telegram, tìm **@BotFather**
2. Gửi `/newbot`, đặt tên và username cho bot
3. Copy **Bot Token** được cung cấp

### 2. Lấy Owner Chat ID

1. Nhắn tin bất kỳ cho bot sau khi chạy
2. Gửi lệnh `/myid` -> bot sẽ trả về Chat ID của bạn
3. Dùng Chat ID này để cấu hình `OWNER_CHAT_ID`

### 3. Cấu hình

Đặt biến môi trường trước khi chạy:

```bash
# Linux / macOS
export BOT_TOKEN=your_bot_token_here
export BOT_USERNAME=your_bot_username
export OWNER_CHAT_ID=your_chat_id

# Windows (PowerShell)
$env:BOT_TOKEN="your_bot_token_here"
$env:BOT_USERNAME="your_bot_username"
$env:OWNER_CHAT_ID="your_chat_id"
```

Hoặc sửa trực tiếp file `src/main/resources/application.yml`.

### 4. Build & Run

```bash
mvn clean package -DskipTests
java -jar target/milktea-telegram-bot-1.0.0.jar
```

Hoặc chạy trực tiếp bằng Maven:

```bash
mvn spring-boot:run
```

## Cấu trúc dự án

```
src/main/java/com/milktea/bot/
├── MilkTeaBotApplication.java      # Entry point
├── config/
│   └── BotConfig.java              # Bot configuration
├── model/
│   ├── MenuItem.java               # Menu item data
│   ├── CartItem.java               # Cart item (item + size + toppings + qty)
│   ├── Order.java                  # Completed order
│   ├── UserSession.java            # Per-user conversation state & cart
│   ├── Size.java                   # M / L enum
│   └── ConversationState.java      # Conversation state machine
├── service/
│   ├── MenuService.java            # Load & query menu from CSV
│   ├── SessionService.java         # Manage user sessions (ConcurrentHashMap)
│   └── OrderService.java           # Create orders, format messages
└── telegram/
    ├── MilkTeaBot.java             # Main bot logic & message handling
    └── KeyboardFactory.java        # Build InlineKeyboard layouts
```

## Luồng đặt hàng

```
/start → Xem Menu → Chọn danh mục → Chọn món → Chọn size
  → Chọn topping (tuỳ chọn) → Chọn số lượng → Thêm vào giỏ
  → [Thêm món / Xem giỏ / Đặt hàng]
  → Xác nhận → Nhập tên + SĐT → Đặt hàng thành công!
  → Bot gửi thông tin đơn cho chủ quán
```

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Telegram API | TelegramBots 6.8.0 |
| Menu Data | CSV (classpath resource) |
| Session | In-memory ConcurrentHashMap |
| Build | Maven |
