package com.milktea.bot.telegram;

import com.milktea.bot.model.CartItem;
import com.milktea.bot.model.MenuItem;
import com.milktea.bot.service.MenuService;
import com.milktea.bot.service.OrderService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {

    private final MenuService menuService;

    public KeyboardFactory(MenuService menuService) {
        this.menuService = menuService;
    }

    public InlineKeyboardMarkup mainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("📋 Xem Menu", "menu"),
                        button("🛒 Giỏ hàng", "cart")
                ))
                .keyboardRow(List.of(button("📖 Hướng dẫn", "help")))
                .build();
    }

    public InlineKeyboardMarkup categories() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<String> cats = menuService.getDrinkCategories();

        for (int i = 0; i < cats.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            String cat1 = cats.get(i);
            row.add(button(menuService.getCategoryEmoji(cat1) + " " + cat1, "cat:" + cat1));
            if (i + 1 < cats.size()) {
                String cat2 = cats.get(i + 1);
                row.add(button(menuService.getCategoryEmoji(cat2) + " " + cat2, "cat:" + cat2));
            }
            rows.add(row);
        }

        rows.add(List.of(button("🛒 Giỏ hàng", "cart")));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup categoryItems(String category) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<MenuItem> items = menuService.getItemsByCategory(category);

        for (MenuItem item : items) {
            String label = String.format("%s — %s / %s",
                    item.getName(),
                    OrderService.formatPrice(item.getPriceM()),
                    OrderService.formatPrice(item.getPriceL()));
            rows.add(List.of(button(label, "item:" + item.getItemId())));
        }

        rows.add(List.of(button("⬅️ Quay lại", "menu")));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup sizeSelection(MenuItem item) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("Size M — " + OrderService.formatPrice(item.getPriceM()), "size:M"),
                        button("Size L — " + OrderService.formatPrice(item.getPriceL()), "size:L")
                ))
                .keyboardRow(List.of(button("⬅️ Quay lại", "menu")))
                .build();
    }

    public InlineKeyboardMarkup toppingSelection(List<MenuItem> allToppings, List<MenuItem> selected) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < allToppings.size(); i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            MenuItem t1 = allToppings.get(i);
            boolean sel1 = selected.stream().anyMatch(s -> s.getItemId().equals(t1.getItemId()));
            row.add(button(
                    (sel1 ? "✅ " : "") + t1.getName() + " +" + OrderService.formatPrice(t1.getPriceM()),
                    "top:" + t1.getItemId()));

            if (i + 1 < allToppings.size()) {
                MenuItem t2 = allToppings.get(i + 1);
                boolean sel2 = selected.stream().anyMatch(s -> s.getItemId().equals(t2.getItemId()));
                row.add(button(
                        (sel2 ? "✅ " : "") + t2.getName() + " +" + OrderService.formatPrice(t2.getPriceM()),
                        "top:" + t2.getItemId()));
            }
            rows.add(row);
        }

        rows.add(List.of(
                button("❌ Không thêm", "top_none"),
                button("✅ Xong topping", "top_done")
        ));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup quantitySelection() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("1", "qty:1"),
                        button("2", "qty:2"),
                        button("3", "qty:3"),
                        button("4", "qty:4"),
                        button("5", "qty:5")
                ))
                .keyboardRow(List.of(button("⬅️ Quay lại menu", "menu")))
                .build();
    }

    public InlineKeyboardMarkup noteSelection() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("🧊 Ít đá", "note:Ít đá"),
                        button("❄️ Nhiều đá", "note:Nhiều đá"),
                        button("🚫 Không đá", "note:Không đá")
                ))
                .keyboardRow(List.of(
                        button("🍬 Ít ngọt", "note:Ít ngọt"),
                        button("🍯 Nhiều ngọt", "note:Nhiều ngọt")
                ))
                .keyboardRow(List.of(
                        button("⏭ Bỏ qua", "note_skip"),
                        button("📝 Ghi chú khác", "note_custom")
                ))
                .build();
    }

    public InlineKeyboardMarkup afterAddToCart() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("📋 Thêm món", "menu"),
                        button("🛒 Giỏ hàng", "cart")
                ))
                .keyboardRow(List.of(button("💰 Đặt hàng ngay", "order")))
                .build();
    }

    public InlineKeyboardMarkup cartActions(List<CartItem> items) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            rows.add(List.of(button(
                    "🗑 Xoá: " + items.get(i).getMenuItem().getName(),
                    "cart_rm:" + i)));
        }

        rows.add(List.of(
                button("📋 Thêm món", "menu"),
                button("🗑 Xoá tất cả", "cart_clear")
        ));

        if (!items.isEmpty()) {
            rows.add(List.of(button("💰 Đặt hàng", "order")));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup orderConfirmation() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("✅ Xác nhận đặt hàng", "confirm"),
                        button("❌ Huỷ", "cancel")
                ))
                .build();
    }

    public InlineKeyboardMarkup backToMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(button("📋 Xem Menu", "menu")))
                .build();
    }

    public InlineKeyboardMarkup orderComplete() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(button("📋 Đặt thêm đơn mới", "start")))
                .build();
    }

    // ==================== OWNER KEYBOARDS ====================

    public InlineKeyboardMarkup ownerOrderActions(String orderId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("✅ Xác nhận làm", "owner_accept:" + orderId),
                        button("❌ Từ chối", "owner_reject:" + orderId)
                ))
                .build();
    }

    public InlineKeyboardMarkup ownerRejectReasons(String orderId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(button("🚫 Hết nguyên liệu", "reject_reason:" + orderId + ":Hết nguyên liệu")))
                .keyboardRow(List.of(button("🕐 Quá tải, không nhận thêm", "reject_reason:" + orderId + ":Quán đang quá tải")))
                .keyboardRow(List.of(button("🔒 Quán sắp đóng cửa", "reject_reason:" + orderId + ":Quán sắp đóng cửa")))
                .keyboardRow(List.of(button("📝 Lý do khác", "reject_custom:" + orderId)))
                .keyboardRow(List.of(button("⬅️ Quay lại", "owner_back:" + orderId)))
                .build();
    }

    public InlineKeyboardMarkup ownerOrderDone(String orderId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        button("🎉 Đã làm xong", "owner_done:" + orderId)
                ))
                .build();
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }
}
