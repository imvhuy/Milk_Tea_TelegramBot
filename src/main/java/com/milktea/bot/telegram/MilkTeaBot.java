package com.milktea.bot.telegram;

import com.milktea.bot.config.BotConfig;
import com.milktea.bot.model.*;
import com.milktea.bot.model.OrderStatus;
import com.milktea.bot.service.MenuService;
import com.milktea.bot.service.OrderService;
import com.milktea.bot.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MilkTeaBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(MilkTeaBot.class);

    private final BotConfig botConfig;
    private final MenuService menuService;
    private final SessionService sessionService;
    private final OrderService orderService;
    private final KeyboardFactory keyboard;

    public MilkTeaBot(BotConfig botConfig, MenuService menuService,
                      SessionService sessionService, OrderService orderService,
                      KeyboardFactory keyboard) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.menuService = menuService;
        this.sessionService = sessionService;
        this.orderService = orderService;
        this.keyboard = keyboard;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    // ==================== TEXT MESSAGE HANDLERS ====================

    private void handleMessage(Message message) {
        long chatId = message.getChatId();
        String text = message.getText().trim();
        UserSession session = sessionService.getSession(chatId);

        switch (text) {
            case "/start" -> handleStart(chatId, session);
            case "/menu"  -> handleMenuCommand(chatId, session);
            case "/cart"  -> handleCartCommand(chatId, session);
            case "/help"  -> handleHelp(chatId);
            case "/myid"  -> sendMessage(chatId, "🆔 Chat ID của bạn: <code>" + chatId + "</code>", null);
            case "/orders" -> handleOrdersCommand(chatId);
            default -> {
                if (session.getState() == ConversationState.OWNER_TYPING_REJECT_REASON
                        && chatId == botConfig.getOwnerChatId()) {
                    String orderId = session.getPendingRejectOrderId();
                    session.setState(ConversationState.IDLE);
                    session.setPendingRejectOrderId(null);
                    processOwnerCustomRejectReason(chatId, orderId, text);
                } else if (session.getState() == ConversationState.ADDING_NOTE) {
                    // Ghi chú tự do bằng text
                    String note = text.length() > 100 ? text.substring(0, 100) : text;
                    CartItem cartItem = new CartItem(
                            session.getSelectedItem(),
                            session.getSelectedSize(),
                            new ArrayList<>(session.getSelectedToppings()),
                            session.getSelectedQuantity()
                    );
                    cartItem.setNote(note);
                    session.addToCart(cartItem);
                    session.setState(ConversationState.IDLE);

                    String toppingInfo = cartItem.getToppings().isEmpty() ? ""
                            : "\n   + " + cartItem.getToppings().stream()
                            .map(MenuItem::getName).collect(Collectors.joining(", "));

                    sendMessage(chatId,
                            String.format("✅ Đã thêm vào giỏ:\n<b>%dx %s (Size %s)</b>%s\n   📝 %s\n→ %s\n\n"
                                            + "🛒 Giỏ hàng: %d món — Tổng: %s",
                                    cartItem.getQuantity(),
                                    cartItem.getMenuItem().getName(),
                                    cartItem.getSize().getLabel(),
                                    toppingInfo,
                                    note,
                                    OrderService.formatPrice(cartItem.getSubtotal()),
                                    session.getCartItemCount(),
                                    OrderService.formatPrice(session.getCartTotal())),
                            keyboard.afterAddToCart());
                } else if (session.getState() == ConversationState.AWAITING_CONTACT) {
                    processContactInfo(chatId, session, text);
                } else {
                    sendMessage(chatId,
                            "Vui lòng sử dụng nút bấm bên dưới hoặc lệnh:\n"
                                    + "/menu - Xem menu\n"
                                    + "/cart - Giỏ hàng\n"
                                    + "/start - Bắt đầu lại",
                            keyboard.mainMenu());
                }
            }
        }
    }

    private void handleStart(long chatId, UserSession session) {
        session.setState(ConversationState.IDLE);
        session.resetCurrentSelection();

        sendMessage(chatId,
                "🧋 <b>Chào mừng đến Quán Trà Sữa!</b>\n\n"
                        + "Mình sẽ giúp bạn đặt hàng nhanh chóng.\n"
                        + "Bấm nút bên dưới để bắt đầu nhé!",
                keyboard.mainMenu());
    }

    private void handleMenuCommand(long chatId, UserSession session) {
        session.setState(ConversationState.SELECTING_CATEGORY);
        sendMessage(chatId, "📋 <b>MENU</b>\nChọn danh mục bạn muốn xem:", keyboard.categories());
    }

    private void handleCartCommand(long chatId, UserSession session) {
        String text = orderService.formatCartSummary(session.getCart());
        sendMessage(chatId, text, keyboard.cartActions(session.getCart()));
    }

    private void handleHelp(long chatId) {
        sendMessage(chatId,
                "📖 <b>HƯỚNG DẪN ĐẶT HÀNG</b>\n\n"
                        + "1️⃣ Bấm <b>Xem Menu</b> để chọn món\n"
                        + "2️⃣ Chọn danh mục → Chọn món → Chọn size\n"
                        + "3️⃣ Thêm topping nếu muốn\n"
                        + "4️⃣ Chọn số lượng\n"
                        + "5️⃣ Bấm <b>Đặt hàng</b> khi chọn xong\n"
                        + "6️⃣ Nhập tên & SĐT để xác nhận\n\n"
                        + "<b>Lệnh hỗ trợ:</b>\n"
                        + "/start - Bắt đầu lại\n"
                        + "/menu - Xem menu\n"
                        + "/cart - Xem giỏ hàng\n"
                        + "/help - Hướng dẫn",
                keyboard.mainMenu());
    }

    // ==================== CALLBACK HANDLERS ====================

    private void handleCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String data = callback.getData();
        UserSession session = sessionService.getSession(chatId);

        answerCb(callback.getId());

        if (data == null || data.isBlank()) return;

        if (data.equals("start")) {
            handleStart(chatId, session);
            return;
        }
        if (data.equals("help")) {
            handleHelp(chatId);
            return;
        }

        switch (data) {
            case "menu" -> showCategories(chatId, messageId, session);
            case "cart" -> showCart(chatId, messageId, session);
            case "cart_clear" -> { session.clearCart(); showCart(chatId, messageId, session); }
            case "order" -> showOrderConfirmation(chatId, messageId, session);
            case "confirm" -> handleOrderConfirmed(chatId, messageId, session);
            case "cancel" -> handleOrderCancelled(chatId, messageId, session);
            case "top_done" -> showQuantitySelection(chatId, messageId, session);
            case "top_none" -> { session.getSelectedToppings().clear(); showQuantitySelection(chatId, messageId, session); }
            case "note_skip" -> addItemToCart(chatId, messageId, session, "");
            case "note_custom" -> {
                session.setState(ConversationState.ADDING_NOTE);
                editMessage(chatId, messageId,
                        "📝 Nhập ghi chú của bạn:\n<i>(VD: ít đường, nhiều đá, để riêng trân châu...)</i>",
                        null);
            }
            default -> handleDynamicCallback(chatId, messageId, session, data);
        }
    }

    private void handleDynamicCallback(long chatId, int messageId, UserSession session, String data) {
        try {
            if (data.startsWith("cat:")) {
                showCategoryItems(chatId, messageId, session, data.substring(4));
            } else if (data.startsWith("item:")) {
                showSizeSelection(chatId, messageId, session, data.substring(5));
            } else if (data.startsWith("size:")) {
                handleSizeSelected(chatId, messageId, session, data.substring(5));
            } else if (data.startsWith("top:")) {
                handleToppingToggle(chatId, messageId, session, data.substring(4));
            } else if (data.startsWith("qty:")) {
                handleQuantitySelected(chatId, messageId, session, Integer.parseInt(data.substring(4)));
            } else if (data.startsWith("note:")) {
                addItemToCart(chatId, messageId, session, data.substring(5));
            } else if (data.startsWith("cart_rm:")) {
                handleRemoveFromCart(chatId, messageId, session, Integer.parseInt(data.substring(8)));
            } else if (data.startsWith("owner_accept:")) {
                handleOwnerAcceptOrder(chatId, messageId, data.substring(13));
            } else if (data.startsWith("owner_reject:")) {
                handleOwnerRejectOrder(chatId, messageId, data.substring(13));
            } else if (data.startsWith("owner_done:")) {
                handleOwnerDoneOrder(chatId, messageId, data.substring(11));
            } else if (data.startsWith("reject_reason:")) {
                // Format: reject_reason:{orderId}:{reason}
                String payload = data.substring(14);
                int sep = payload.indexOf(":");
                if (sep > 0) {
                    handleOwnerRejectWithReason(chatId, messageId,
                            payload.substring(0, sep), payload.substring(sep + 1));
                }
            } else if (data.startsWith("owner_back:")) {
                handleOwnerBackToActions(chatId, messageId, data.substring(11));
            } else if (data.startsWith("reject_custom:")) {
                handleOwnerCustomReject(chatId, messageId, session, data.substring(14));
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid callback data: {}", data);
        }
    }

    // ==================== MENU NAVIGATION ====================

    private void showCategories(long chatId, int messageId, UserSession session) {
        session.setState(ConversationState.SELECTING_CATEGORY);
        session.resetCurrentSelection();
        editMessage(chatId, messageId, "📋 <b>MENU</b>\nChọn danh mục:", keyboard.categories());
    }

    private void showCategoryItems(long chatId, int messageId, UserSession session, String category) {
        session.setState(ConversationState.SELECTING_ITEM);
        String emoji = menuService.getCategoryEmoji(category);
        editMessage(chatId, messageId,
                emoji + " <b>" + category + "</b>\nChọn món (giá Size M / Size L):",
                keyboard.categoryItems(category));
    }

    private void showSizeSelection(long chatId, int messageId, UserSession session, String itemId) {
        MenuItem item = menuService.getItemById(itemId);
        if (item == null) return;

        session.setState(ConversationState.SELECTING_SIZE);
        session.setSelectedItem(item);

        editMessage(chatId, messageId,
                String.format("🧋 <b>%s</b>\n<i>%s</i>\n\nChọn size:",
                        item.getName(), item.getDescription()),
                keyboard.sizeSelection(item));
    }

    // ==================== ITEM CONFIGURATION ====================

    private void handleSizeSelected(long chatId, int messageId, UserSession session, String sizeStr) {
        if (session.getSelectedItem() == null) {
            showCategories(chatId, messageId, session);
            return;
        }
        Size size;
        try {
            size = Size.valueOf(sizeStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid size value: {}", sizeStr);
            return;
        }
        session.setSelectedSize(size);
        session.setState(ConversationState.SELECTING_TOPPING);

        List<MenuItem> toppings = menuService.getAvailableToppings();

        editMessage(chatId, messageId,
                String.format("🧋 %s (Size %s) — %s\n\n🧁 Chọn topping (bấm để chọn/bỏ):",
                        session.getSelectedItem().getName(),
                        size.getLabel(),
                        OrderService.formatPrice(session.getSelectedItem().getPrice(size))),
                keyboard.toppingSelection(toppings, session.getSelectedToppings()));
    }

    private void handleToppingToggle(long chatId, int messageId, UserSession session, String toppingId) {
        if (session.getSelectedItem() == null || session.getSelectedSize() == null) {
            showCategories(chatId, messageId, session);
            return;
        }
        MenuItem topping = menuService.getItemById(toppingId);
        if (topping == null) return;

        List<MenuItem> selected = session.getSelectedToppings();
        boolean alreadySelected = selected.stream()
                .anyMatch(t -> t.getItemId().equals(toppingId));

        if (alreadySelected) {
            selected.removeIf(t -> t.getItemId().equals(toppingId));
        } else {
            selected.add(topping);
        }

        List<MenuItem> allToppings = menuService.getAvailableToppings();
        int toppingTotal = selected.stream().mapToInt(MenuItem::getPriceM).sum();
        int itemPrice = session.getSelectedItem().getPrice(session.getSelectedSize());

        String selectedNames = selected.isEmpty() ? "Chưa chọn"
                : selected.stream().map(MenuItem::getName).collect(Collectors.joining(", "))
                + " (+" + OrderService.formatPrice(toppingTotal) + ")";

        editMessage(chatId, messageId,
                String.format("🧋 %s (Size %s) — %s\n🧁 Topping: %s\n\nBấm để chọn/bỏ topping:",
                        session.getSelectedItem().getName(),
                        session.getSelectedSize().getLabel(),
                        OrderService.formatPrice(itemPrice),
                        selectedNames),
                keyboard.toppingSelection(allToppings, selected));
    }

    private void showQuantitySelection(long chatId, int messageId, UserSession session) {
        if (session.getSelectedItem() == null || session.getSelectedSize() == null) {
            showCategories(chatId, messageId, session);
            return;
        }
        session.setState(ConversationState.SELECTING_QUANTITY);

        int unitPrice = session.getSelectedItem().getPrice(session.getSelectedSize())
                + session.getSelectedToppings().stream().mapToInt(MenuItem::getPriceM).sum();

        String toppingStr = session.getSelectedToppings().isEmpty() ? ""
                : "\n🧁 Topping: " + session.getSelectedToppings().stream()
                .map(MenuItem::getName).collect(Collectors.joining(", "));

        editMessage(chatId, messageId,
                String.format("🧋 %s (Size %s)%s\n💲 Đơn giá: %s\n\nChọn số lượng:",
                        session.getSelectedItem().getName(),
                        session.getSelectedSize().getLabel(),
                        toppingStr,
                        OrderService.formatPrice(unitPrice)),
                keyboard.quantitySelection());
    }

    private void handleQuantitySelected(long chatId, int messageId, UserSession session, int quantity) {
        if (session.getSelectedItem() == null || session.getSelectedSize() == null) {
            showCategories(chatId, messageId, session);
            return;
        }
        session.setSelectedQuantity(quantity);
        session.setState(ConversationState.ADDING_NOTE);

        editMessage(chatId, messageId,
                String.format("🧋 %dx %s (Size %s)\n📝 Bạn có ghi chú gì không?",
                        quantity,
                        session.getSelectedItem().getName(),
                        session.getSelectedSize().getLabel()),
                keyboard.noteSelection());
    }

    private void addItemToCart(long chatId, int messageId, UserSession session, String note) {
        CartItem cartItem = new CartItem(
                session.getSelectedItem(),
                session.getSelectedSize(),
                new ArrayList<>(session.getSelectedToppings()),
                session.getSelectedQuantity()
        );
        if (!note.isEmpty()) {
            cartItem.setNote(note);
        }

        session.addToCart(cartItem);
        session.setState(ConversationState.IDLE);

        String toppingInfo = cartItem.getToppings().isEmpty() ? ""
                : "\n   + " + cartItem.getToppings().stream()
                .map(MenuItem::getName).collect(Collectors.joining(", "));

        String noteInfo = cartItem.getNote().isEmpty() ? ""
                : "\n   📝 " + cartItem.getNote();

        editMessage(chatId, messageId,
                String.format("✅ Đã thêm vào giỏ:\n<b>%dx %s (Size %s)</b>%s%s\n→ %s\n\n"
                                + "🛒 Giỏ hàng: %d món — Tổng: %s",
                        cartItem.getQuantity(),
                        cartItem.getMenuItem().getName(),
                        cartItem.getSize().getLabel(),
                        toppingInfo,
                        noteInfo,
                        OrderService.formatPrice(cartItem.getSubtotal()),
                        session.getCartItemCount(),
                        OrderService.formatPrice(session.getCartTotal())),
                keyboard.afterAddToCart());
    }

    // ==================== CART ====================

    private void showCart(long chatId, int messageId, UserSession session) {
        session.setState(ConversationState.VIEWING_CART);
        String text = orderService.formatCartSummary(session.getCart());
        editMessage(chatId, messageId, text, keyboard.cartActions(session.getCart()));
    }

    private void handleRemoveFromCart(long chatId, int messageId, UserSession session, int index) {
        if (index >= 0 && index < session.getCart().size()) {
            String removedName = session.getCart().get(index).getMenuItem().getName();
            session.removeFromCart(index);
            String text = "🗑 Đã xoá: " + removedName + "\n\n"
                    + orderService.formatCartSummary(session.getCart());
            editMessage(chatId, messageId, text, keyboard.cartActions(session.getCart()));
        }
    }

    // ==================== ORDER ====================

    private void showOrderConfirmation(long chatId, int messageId, UserSession session) {
        if (session.isCartEmpty()) {
            editMessage(chatId, messageId,
                    "🛒 Giỏ hàng trống!\nHãy chọn món trước khi đặt hàng nhé.",
                    keyboard.backToMenu());
            return;
        }

        session.setState(ConversationState.CONFIRMING_ORDER);
        String text = orderService.formatCartSummary(session.getCart())
                + "\n\n❓ Xác nhận đặt hàng?";
        editMessage(chatId, messageId, text, keyboard.orderConfirmation());
    }

    private void handleOrderConfirmed(long chatId, int messageId, UserSession session) {
        session.setState(ConversationState.AWAITING_CONTACT);
        editMessage(chatId, messageId,
                "📝 Vui lòng gửi <b>tên</b> và <b>số điện thoại</b> để xác nhận đơn hàng.\n\n"
                        + "<i>Ví dụ: Huy - 0912345678</i>",
                null);
    }

    private void handleOrderCancelled(long chatId, int messageId, UserSession session) {
        session.setState(ConversationState.IDLE);
        editMessage(chatId, messageId,
                "❌ Đã huỷ. Giỏ hàng vẫn được giữ nguyên.",
                keyboard.mainMenu());
    }

    private void processContactInfo(long chatId, UserSession session, String text) {
        String name;
        String phone;

        String[] parts = text.split("[\\-–—]");
        if (parts.length >= 2) {
            name = parts[0].trim();
            phone = parts[parts.length - 1].trim().replaceAll("[^0-9]", "");
        } else {
            String[] words = text.trim().split("\\s+");
            phone = "";
            StringBuilder nameBuilder = new StringBuilder();
            for (String word : words) {
                String digits = word.replaceAll("[^0-9]", "");
                if (digits.length() >= 8) {
                    phone = digits;
                } else {
                    if (!nameBuilder.isEmpty()) nameBuilder.append(" ");
                    nameBuilder.append(word);
                }
            }
            name = nameBuilder.toString().trim();
            if (name.isEmpty()) name = "Khách";
        }

        if (phone.isEmpty() || phone.length() < 8 || phone.length() > 11) {
            sendMessage(chatId,
                    "⚠️ Chưa nhận được SĐT hợp lệ.\nVui lòng nhập lại theo mẫu:\n<i>Tên - SĐT</i>\n\n"
                            + "<i>Ví dụ: Huy - 0912345678</i>",
                    null);
            return;
        }

        if (session.isCartEmpty()) {
            session.setState(ConversationState.IDLE);
            sendMessage(chatId, "⚠️ Giỏ hàng đã trống. Vui lòng đặt hàng lại.", keyboard.mainMenu());
            return;
        }

        Order order = orderService.createOrder(chatId, name, phone, session.getCart());

        sendMessage(chatId, orderService.formatOrderForCustomer(order), keyboard.orderComplete());

        notifyOwner(order);

        session.clearCart();
        session.resetCurrentSelection();
        session.setState(ConversationState.IDLE);

        log.info("Order {} placed by {} (chatId={})", order.getOrderId(), name, chatId);
    }

    private void notifyOwner(Order order) {
        long ownerChatId = botConfig.getOwnerChatId();
        if (ownerChatId == 0) {
            log.warn("Owner chat ID not configured! Order {} not forwarded.", order.getOrderId());
            return;
        }
        sendMessage(ownerChatId, orderService.formatOrderForOwner(order),
                keyboard.ownerOrderActions(order.getOrderId()));
    }

    // ==================== OWNER COMMANDS ====================

    private void handleOrdersCommand(long chatId) {
        if (chatId != botConfig.getOwnerChatId()) {
            sendMessage(chatId, "⚠️ Bạn không có quyền xem danh sách đơn hàng.", null);
            return;
        }

        List<Order> pending = orderService.getPendingOrders();
        if (pending.isEmpty()) {
            sendMessage(chatId, "📋 Không có đơn hàng nào đang chờ.", null);
            return;
        }

        sendMessage(chatId, "📋 <b>CÓ " + pending.size() + " ĐƠN ĐANG CHỜ:</b>", null);
        for (Order order : pending) {
            sendMessage(chatId, orderService.formatOrderForOwner(order),
                    keyboard.ownerOrderActions(order.getOrderId()));
        }
    }

    private void handleOwnerAcceptOrder(long chatId, int messageId, String orderId) {
        if (chatId != botConfig.getOwnerChatId()) return;

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            editMessage(chatId, messageId, "⚠️ Không tìm thấy đơn " + orderId, null);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            editMessage(chatId, messageId,
                    orderService.formatOrderForOwner(order) + "\n\n" + order.getStatus().getLabel(),
                    null);
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);

        // Cập nhật tin nhắn owner — hiện nút "Đã làm xong"
        editMessage(chatId, messageId,
                orderService.formatOrderForOwner(order) + "\n\n🔥 <b>ĐANG PHA CHẾ</b>",
                keyboard.ownerOrderDone(order.getOrderId()));

        // Thông báo cho khách
        sendMessage(order.getCustomerChatId(),
                "✅ <b>Đơn hàng " + orderId + " đã được xác nhận!</b>\n"
                        + "⏰ Quán đang pha chế, vui lòng chờ 10-15 phút nhé.\n"
                        + "Cảm ơn bạn! 🙏",
                null);

        log.info("Order {} CONFIRMED by owner", orderId);
    }

    private void handleOwnerRejectOrder(long chatId, int messageId, String orderId) {
        if (chatId != botConfig.getOwnerChatId()) return;

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            editMessage(chatId, messageId, "⚠️ Không tìm thấy đơn " + orderId, null);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            editMessage(chatId, messageId,
                    orderService.formatOrderForOwner(order) + "\n\n" + order.getStatus().getLabel(),
                    null);
            return;
        }

        // Hiện danh sách lý do từ chối
        editMessage(chatId, messageId,
                orderService.formatOrderForOwner(order) + "\n\n❓ <b>Chọn lý do từ chối:</b>",
                keyboard.ownerRejectReasons(orderId));
    }

    private void handleOwnerRejectWithReason(long chatId, int messageId, String orderId, String reason) {
        if (chatId != botConfig.getOwnerChatId()) return;

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            editMessage(chatId, messageId, "⚠️ Không tìm thấy đơn " + orderId, null);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            editMessage(chatId, messageId,
                    orderService.formatOrderForOwner(order) + "\n\n" + order.getStatus().getLabel(),
                    null);
            return;
        }

        order.setStatus(OrderStatus.REJECTED);

        // Cập nhật tin nhắn owner
        editMessage(chatId, messageId,
                orderService.formatOrderForOwner(order) + "\n\n❌ <b>ĐÃ TỪ CHỐI</b>\nLý do: " + reason,
                null);

        // Thông báo cho khách kèm lý do
        sendMessage(order.getCustomerChatId(),
                "😔 <b>Đơn hàng " + orderId + " không thể thực hiện.</b>\n"
                        + "📋 Lý do: <i>" + reason + "</i>\n\n"
                        + "Vui lòng thử lại sau hoặc liên hệ quán nhé!",
                keyboard.mainMenu());

        log.info("Order {} REJECTED by owner, reason: {}", orderId, reason);
    }

    private void handleOwnerBackToActions(long chatId, int messageId, String orderId) {
        if (chatId != botConfig.getOwnerChatId()) return;

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            editMessage(chatId, messageId, "⚠️ Không tìm thấy đơn " + orderId, null);
            return;
        }

        editMessage(chatId, messageId,
                orderService.formatOrderForOwner(order),
                keyboard.ownerOrderActions(orderId));
    }

    private void handleOwnerCustomReject(long chatId, int messageId, UserSession session, String orderId) {
        if (chatId != botConfig.getOwnerChatId()) return;

        session.setPendingRejectOrderId(orderId);
        session.setState(ConversationState.OWNER_TYPING_REJECT_REASON);

        editMessage(chatId, messageId,
                "📝 Nhập lý do từ chối đơn <b>" + orderId + "</b>:",
                null);
    }

    private void processOwnerCustomRejectReason(long chatId, String orderId, String reason) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            sendMessage(chatId, "⚠️ Không tìm thấy đơn " + orderId, null);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            sendMessage(chatId, "⚠️ Đơn " + orderId + " đã được xử lý: " + order.getStatus().getLabel(), null);
            return;
        }

        order.setStatus(OrderStatus.REJECTED);

        sendMessage(chatId,
                "❌ Đã từ chối đơn <b>" + orderId + "</b>\nLý do: " + reason,
                null);

        sendMessage(order.getCustomerChatId(),
                "😔 <b>Đơn hàng " + orderId + " không thể thực hiện.</b>\n"
                        + "📋 Lý do: <i>" + reason + "</i>\n\n"
                        + "Vui lòng thử lại sau hoặc liên hệ quán nhé!",
                keyboard.mainMenu());

        log.info("Order {} REJECTED by owner, custom reason: {}", orderId, reason);
    }

    private void handleOwnerDoneOrder(long chatId, int messageId, String orderId) {
        if (chatId != botConfig.getOwnerChatId()) return;

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            editMessage(chatId, messageId, "⚠️ Không tìm thấy đơn " + orderId, null);
            return;
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            editMessage(chatId, messageId,
                    orderService.formatOrderForOwner(order) + "\n\n" + order.getStatus().getLabel(),
                    null);
            return;
        }

        order.setStatus(OrderStatus.COMPLETED);

        // Cập nhật tin nhắn owner — xong, ẩn nút
        editMessage(chatId, messageId,
                orderService.formatOrderForOwner(order) + "\n\n🎉 <b>ĐÃ XONG</b>",
                null);

        // Thông báo khách đến lấy
        sendMessage(order.getCustomerChatId(),
                "🎉 <b>Đơn hàng " + orderId + " đã làm xong!</b>\n"
                        + "Bạn có thể đến quán lấy đồ uống nhé.\n"
                        + "Cảm ơn bạn! 🧋",
                null);

        log.info("Order {} COMPLETED by owner", orderId);
    }

    // ==================== TELEGRAM API HELPERS ====================

    private void sendMessage(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setParseMode("HTML");
        if (markup != null) {
            msg.setReplyMarkup(markup);
        }
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId={}", chatId, e);
        }
    }

    private void editMessage(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("HTML");
        if (markup != null) {
            edit.setReplyMarkup(markup);
        }
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("message is not modified")) {
                return; // ignore duplicate edits
            }
            log.error("Failed to edit message msgId={} chatId={}", messageId, chatId, e);
        }
    }

    private void answerCb(String callbackQueryId) {
        try {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query", e);
        }
    }
}
