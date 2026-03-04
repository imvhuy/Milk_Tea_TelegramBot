package com.milktea.bot.model;

import java.util.ArrayList;
import java.util.List;

public class UserSession {

    private final long chatId;
    private ConversationState state = ConversationState.IDLE;
    private final List<CartItem> cart = new ArrayList<>();

    private MenuItem selectedItem;
    private Size selectedSize;
    private final List<MenuItem> selectedToppings = new ArrayList<>();
    private int selectedQuantity;
    private String pendingRejectOrderId;

    public UserSession(long chatId) {
        this.chatId = chatId;
    }

    public long getChatId() { return chatId; }
    public ConversationState getState() { return state; }
    public void setState(ConversationState state) { this.state = state; }
    public List<CartItem> getCart() { return cart; }

    public MenuItem getSelectedItem() { return selectedItem; }
    public void setSelectedItem(MenuItem item) { this.selectedItem = item; }
    public Size getSelectedSize() { return selectedSize; }
    public void setSelectedSize(Size size) { this.selectedSize = size; }
    public List<MenuItem> getSelectedToppings() { return selectedToppings; }
    public int getSelectedQuantity() { return selectedQuantity; }
    public void setSelectedQuantity(int qty) { this.selectedQuantity = qty; }
    public String getPendingRejectOrderId() { return pendingRejectOrderId; }
    public void setPendingRejectOrderId(String orderId) { this.pendingRejectOrderId = orderId; }

    public void resetCurrentSelection() {
        selectedItem = null;
        selectedSize = null;
        selectedToppings.clear();
        selectedQuantity = 0;
    }

    public void addToCart(CartItem item) {
        cart.add(item);
        resetCurrentSelection();
    }

    public void removeFromCart(int index) {
        if (index >= 0 && index < cart.size()) {
            cart.remove(index);
        }
    }

    public void clearCart() {
        cart.clear();
    }

    public int getCartTotal() {
        return cart.stream().mapToInt(CartItem::getSubtotal).sum();
    }

    public int getCartItemCount() {
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public boolean isCartEmpty() {
        return cart.isEmpty();
    }
}
