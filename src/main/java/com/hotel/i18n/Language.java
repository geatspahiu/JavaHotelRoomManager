package com.hotel.i18n;

public enum Language {
    EN("English"),
    SQ("Shqip"),
    SR("Srpski");

    private final String label;

    Language(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
