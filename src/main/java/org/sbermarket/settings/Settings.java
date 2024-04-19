package org.sbermarket.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sbermarket.market.Category;
import org.sbermarket.market.Store;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Settings {
    private final String seleniumGridUrl;
    private final String sbermarketUrl;
    private final Integer numberOfThreads;
    private final HashSet<Store> stores = new HashSet<>();
    private final Logger logger = LogManager.getLogger(Settings.class);

    public Settings() {
        this.sbermarketUrl = System.getenv("SBERMARKET_URL");
        this.seleniumGridUrl = System.getenv("GRID_URL");
        this.numberOfThreads = Integer.parseInt(System.getenv("THREADS"));
        initializeStores();
        logger.info(String.format(
            "Конфигурация. Кол-во магазинов: %d; кол-во потоков: %d",
            stores.size(),
            numberOfThreads
        ));
    }

    public String getSbermarketUrl() {
        return sbermarketUrl;
    }

    public HashSet<Store> getStores() {
        return stores;
    }

    public URL getSeleniumGridUrl() {
        try {
            return new URI(seleniumGridUrl).toURL();
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            logger.fatal("Невалидная ссылка драйвера");
            return null;
        }
    }

    public Integer getNumberOfThreads() {
        return this.numberOfThreads;
    }

    private void initializeStores() {
        JSONObject storeConfiguration = loadJSONFromResource("stores.json");
        storeConfiguration.keySet()
            .forEach(key -> stores.add(getStoreFromJson(storeConfiguration.getJSONObject(key), key)));
    }

    private Store getStoreFromJson(JSONObject store, String sid) {
        JSONArray jsonCategories = store.getJSONArray("categories");
        List<Category> categories = new ArrayList<>();

        for (int i = 0; i < jsonCategories.length(); i++) {
            JSONObject category = jsonCategories.getJSONObject(i);
            categories.add(new Category(getSbermarketUrl() + category.getString("link"),
                category.getString("category"),
                category.getString("subCategory"),
                category.getBoolean("isAdult")
            ));
        }

        return new Store(Integer.parseInt(sid), store.getString("store"), categories);
    }

    private JSONObject loadJSONFromResource(String fileName) {
        try (InputStream inputStream = Settings.class.getClassLoader()
            .getResourceAsStream(fileName)) {
            if (inputStream == null) {
                logger.fatal("Файл конфиуграции магазинов не найден");
                System.exit(1);
            }

            byte[] bytes = inputStream.readAllBytes();
            String jsonString = new String(bytes, StandardCharsets.UTF_8);

            return new JSONObject(jsonString);
        } catch (IOException e) {
            logger.fatal("Произошла ошибка при обработке файла конфигурации магазинов");
            System.exit(1);
            return null;
        }
    }
}
