package com.milktea.bot.model;

public class MenuItem {

    private final String category;
    private final String itemId;
    private final String name;
    private final String description;
    private final int priceM;
    private final int priceL;
    private final boolean available;

    public MenuItem(String category, String itemId, String name, String description,
                    int priceM, int priceL, boolean available) {
        this.category = category;
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.priceM = priceM;
        this.priceL = priceL;
        this.available = available;
    }

    public String getCategory() { return category; }
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPriceM() { return priceM; }
    public int getPriceL() { return priceL; }
    public boolean isAvailable() { return available; }

    public int getPrice(Size size) {
        return size == Size.M ? priceM : priceL;
    }

    public boolean isTopping() {
        return "Topping".equals(category);
    }
}
