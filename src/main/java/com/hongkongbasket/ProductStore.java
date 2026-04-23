package com.hongkongbasket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductStore {
    public static List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("P001", "Amul Milk", "Fresh cow milk in a 1L pack", 28, "Dairy", "images/P001.jpg"));
        products.add(new Product("P002", "Brown Bread", "Whole wheat loaf, freshly baked", 38, "Bakery", "images/P002.jpg"));
        products.add(new Product("P003", "Farm Eggs", "12 fresh farm eggs", 58, "Eggs", "images/P003.jpg"));
        products.add(new Product("P004", "Surf Detergent", "Powerful laundry detergent", 110, "Household", "images/P004.jpg"));
        products.add(new Product("P005", "Coca Cola", "500ml soft drink bottle", 42, "Beverages", "images/P005.jpg"));
        products.add(new Product("P006", "Fresh Tomatoes", "Ripe red tomatoes per kg", 30, "Vegetables", "images/P006.jpg"));
        products.add(new Product("P007", "Yellow Onions", "Golden onions per kg", 26, "Vegetables", "images/P007.jpg"));
        products.add(new Product("P008", "Potatoes", "Starchy potatoes per kg", 22, "Vegetables", "images/P008.jpg"));
        products.add(new Product("P009", "Fresh Carrots", "Crisp orange carrots per kg", 34, "Vegetables", "images/P009.jpg"));
        products.add(new Product("P010", "Broccoli", "Green broccoli per piece", 52, "Vegetables", "images/P010.jpg"));
        products.add(new Product("P011", "Spinach", "Fresh spinach bunch", 28, "Vegetables", "images/P011.jpg"));
        products.add(new Product("P012", "Bell Peppers", "Mixed bell peppers per kg", 48, "Vegetables", "images/P012.jpg"));
        return Collections.unmodifiableList(products);
    }
}
