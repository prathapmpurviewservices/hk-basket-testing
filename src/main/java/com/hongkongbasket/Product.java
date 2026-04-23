package com.hongkongbasket;

public class Product {
    private final String id;
    private final String name;
    private final String description;
    private final int priceHkd;
    private final String category;
    private final String imageUrl;

    public Product(String id, String name, String description, int priceHkd, String category, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priceHkd = priceHkd;
        this.category = category;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPriceHkd() {
        return priceHkd;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
