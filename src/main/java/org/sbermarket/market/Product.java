package org.sbermarket.market;

import java.util.Objects;

public class Product {
    public final String name;
    public final String link;
    public final Double price;
    public final Double discountedPrice;

    public Product(String name, String link, Double price, Double discountedPrice) {
        this.name = name;
        this.link = link;
        this.price = price;
        this.discountedPrice = discountedPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(name, product.name) && Objects.equals(link, product.link) && Objects.equals(price, product.price) && Objects.equals(
            discountedPrice,
            product.discountedPrice
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, link, price, discountedPrice);
    }
}
