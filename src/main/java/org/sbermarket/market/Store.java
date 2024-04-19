package org.sbermarket.market;

import java.util.*;

public class Store {
    public final Integer sid;
    public final String storeName;
    public final List<Category> categories;

    public Store(Integer sid, String storeName, List<Category> categories) {
        this.sid = sid;
        this.storeName = storeName;
        this.categories = categories;
    }

    public List<List<String>> createCSVReport() {
        return categories.stream()
            .flatMap(category -> category.getProducts()
                .stream()
                .map(product -> List.of(
                    storeName,
                    "-",
                    category.category,
                    category.subCategory,
                    product.name,
                    product.price == null ? "-" : product.price.toString(),
                    product.discountedPrice == null ? "-" : product.discountedPrice.toString(),
                    product.link
                )))
            .toList();
    }

    public static List<String> getCsvHeaders() {
        return List.of("Магазин", "Адресс", "Категория", "Подкатегория", "Наименование товара", "Цена со скидкой", "Цена без скидки", "Ссылка на товар");
    }

    /**
     * Разбивает список магазинов на равные части для того,
     * чтобы каждый поток собирал данные с равной части магазинов
     *
     * @param set список магазинов
     * @param numBatches кол-во равный частей
     * @param <T> тип списка
     * @return равные части магазинов
     */
    public static <T> List<HashSet<T>> splitIntoBatches(HashSet<T> set, int numBatches) {
        int batchSize = set.size() / numBatches;
        List<HashSet<T>> batches = new ArrayList<>();
        HashSet<T> currentBatch = new HashSet<>();
        int count = 0;

        for (T item : set) {
            if (count == batchSize && batches.size() < numBatches - 1) {
                batches.add(currentBatch);
                currentBatch = new HashSet<>();
                count = 0;
            }
            currentBatch.add(item);
            count++;
        }

        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        return batches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store store = (Store) o;
        return Objects.equals(sid, store.sid) && Objects.equals(storeName, store.storeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sid, storeName);
    }
}
