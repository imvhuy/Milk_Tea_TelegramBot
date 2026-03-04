package com.milktea.bot.model;

import java.util.ArrayList;
import java.util.List;

public class CartItem {

    private final MenuItem menuItem;
    private final Size size;
    private final List<MenuItem> toppings;
    private final int quantity;
    private String note;

    public CartItem(MenuItem menuItem, Size size, List<MenuItem> toppings, int quantity) {
        this.menuItem = menuItem;
        this.size = size;
        this.toppings = new ArrayList<>(toppings);
        this.quantity = quantity;
        this.note = "";
    }

    public MenuItem getMenuItem() { return menuItem; }
    public Size getSize() { return size; }
    public List<MenuItem> getToppings() { return toppings; }
    public int getQuantity() { return quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getUnitPrice() {
        int base = menuItem.getPrice(size);
        int toppingPrice = toppings.stream().mapToInt(MenuItem::getPriceM).sum();
        return base + toppingPrice;
    }

    public int getSubtotal() {
        return getUnitPrice() * quantity;
    }
}
