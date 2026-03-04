package com.milktea.bot.model;

public enum OrderStatus {
    PENDING("⏳ Chờ xác nhận"),
    CONFIRMED("🔥 Đang pha chế"),
    COMPLETED("✅ Đã xong"),
    REJECTED("❌ Đã từ chối");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}

