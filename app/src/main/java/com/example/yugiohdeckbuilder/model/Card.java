package com.example.yugiohdeckbuilder.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Card implements Serializable {
    private int id;
    private String name;
    private String type;
    private String desc;

    @SerializedName("card_images")
    private List<CardImage> cardImages;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDesc() { return desc; }
    public List<CardImage> getCardImages() { return cardImages; }
}