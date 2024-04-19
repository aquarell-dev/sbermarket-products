package org.sbermarket.market;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Category {
    public final String link;
    public final String category;
    public final String subCategory;
    public final Boolean isAdult;
    private final List<Product> products = new ArrayList<>();

    public Category(String link, String category, String subCategory, Boolean isAdult) {
        this.link = link;
        this.category = category;
        this.subCategory = subCategory;
        this.isAdult = isAdult;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void addProducts(List<Product> products) {
        this.products.addAll(products);
    }
}
