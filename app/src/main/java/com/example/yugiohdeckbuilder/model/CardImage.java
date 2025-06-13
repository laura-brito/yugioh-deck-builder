package com.example.yugiohdeckbuilder.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class CardImage implements Serializable {
    @SerializedName("image_url")
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }
}