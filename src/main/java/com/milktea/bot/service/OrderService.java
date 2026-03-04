package com.milktea.bot.service;

import com.milktea.bot.model.CartItem;
import com.milktea.bot.model.Order;
import com.milktea.bot.model.OrderStatus;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final AtomicInteger orderCounter = new AtomicInteger(0);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();

    public Order createOrder(long chatId, String name, String phone, List<CartItem> items) {
        String orderId = String.format("#%04d", orderCounter.incrementAndGet());
        Order order = new Order(orderId, chatId, name, phone, items);
        orderStore.put(orderId, order);
        return order;
    }

    public Order getOrderById(String orderId) {
        return orderStore.get(orderId);
    }

    public List<Order> getPendingOrders() {
        return orderStore.values().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public String formatOrderForCustomer(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ <b>ĐẶT HÀNG THÀNH CÔNG!</b>\n\n");
        sb.append("📝 Mã đơn: <b>").append(order.getOrderId()).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        appendOrderItems(sb, order.getItems());
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("💰 <b>Tổng cộng: ").append(formatPrice(order.getTotalPrice())).append("</b>\n\n");
        sb.append("⏰ Vui lòng chờ 10-15 phút.\n");
        sb.append("Cảm ơn bạn đã đặt hàng! 🙏");
        return sb.toString();
    }

    public String formatOrderForOwner(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 <b>ĐƠN HÀNG MỚI ").append(order.getOrderId()).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("👤 Khách: <b>").append(escapeHtml(order.getCustomerName())).append("</b>\n");
        sb.append("📱 SĐT: <b>").append(order.getCustomerPhone()).append("</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        appendOrderItems(sb, order.getItems());
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("💰 <b>Tổng cộng: ").append(formatPrice(order.getTotalPrice())).append("</b>\n");
        sb.append("🕐 ").append(order.getCreatedAt().format(TIME_FMT));
        return sb.toString();
    }

    public String formatCartSummary(List<CartItem> items) {
        if (items.isEmpty()) {
            return "🛒 Giỏ hàng trống!\nHãy chọn món từ menu nhé.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🛒 <b>GIỎ HÀNG CỦA BẠN</b>\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        appendOrderItems(sb, items);
        int total = items.stream().mapToInt(CartItem::getSubtotal).sum();
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("💰 <b>Tổng cộng: ").append(formatPrice(total)).append("</b>");
        return sb.toString();
    }

    private void appendOrderItems(StringBuilder sb, List<CartItem> items) {
        for (int i = 0; i < items.size(); i++) {
            CartItem item = items.get(i);
            sb.append(i + 1).append(". ")
              .append(item.getQuantity()).append("x ")
              .append(item.getMenuItem().getName())
              .append(" (").append(item.getSize().getLabel()).append(")\n");

            if (!item.getToppings().isEmpty()) {
                String toppings = item.getToppings().stream()
                        .map(t -> t.getName())
                        .collect(Collectors.joining(", "));
                sb.append("   + ").append(toppings).append("\n");
            }

            if (item.getNote() != null && !item.getNote().isEmpty()) {
                sb.append("   📝 ").append(item.getNote()).append("\n");
            }

            sb.append("   → ").append(formatPrice(item.getSubtotal())).append("\n");
            if (i < items.size() - 1) sb.append("\n");
        }
    }

    public static String formatPrice(int price) {
        return String.format("%,d", price).replace(',', '.') + "đ";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
