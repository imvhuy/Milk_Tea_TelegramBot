package com.milktea.bot.model;

public enum Size {
    M("M"),
    L("L");

    private final String label;

    Size(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
