package com.example.yugiohdeckbuilder.model;

public class LanguageItem {
    private String languageName;
    private String languageCode;
    private int flagImage;

    public LanguageItem(String languageName, String languageCode, int flagImage) {
        this.languageName = languageName;
        this.languageCode = languageCode;
        this.flagImage = flagImage;
    }

    public String getLanguageName() {
        return languageName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public int getFlagImage() {
        return flagImage;
    }
}