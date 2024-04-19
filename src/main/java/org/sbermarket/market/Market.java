package org.sbermarket.market;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.sbermarket.settings.Settings;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Market {
    private final Settings settings;
    private final Logger logger = LogManager.getLogger(Market.class);
    private final HashSet<Store> stores;
    private WebDriver driver;

    public Market(Settings settings, HashSet<Store> stores) {
        this.settings = settings;
        this.stores = stores;
    }

    public void start() {
        this.driver = getDriver();
        logger.info(String.format("Драйвер успешно запущен. (stores=%d)", stores.size()));
        stores.forEach(store -> store.categories.forEach(category -> getProducts(store, category)));
        this.driver.quit();
    }

    private WebDriver getDriver() {
        ChromeOptions capabilities = new ChromeOptions();
        capabilities.addArguments("--window-size=1920,1080");

        return new RemoteWebDriver(settings.getSeleniumGridUrl(), capabilities);
    }

    /**
     * Получаем список товаров этой категории
     * Обход начинаем со второй страницы, ибо товары с первой страницы собраны в методе getPagesCount()
     *
     * @param category категория товаров
     */
    private void getProducts(Store store, Category category) {
        Integer pages = getPagesCount(category);

        if (pages == null) return;

        if (pages == 1) {
            logger.info(String.format(
                "Магазин %s(sid=%d): Собраны товары в категории '%s' с 1 страницы.",
                store.storeName,
                store.sid,
                category.subCategory
            ));
        }

        for (int page = 2; page <= pages; page++) {
            List<Product> products = getProductsFromPage(category.link, page);
            if (products != null) category.addProducts(products);
        }

        logger.info(String.format(
            "Магазин %s(sid=%d): Собраны товары в категории '%s' с %d страниц.",
            store.storeName,
            store.sid,
            category.subCategory,
            pages
        ));
    }

    /**
     * Получает кол-во страниц для каждой категории для дальнейшего парсинга товаров,
     * а также парсит продукты с первой страницы, чтоб не открывать эту страницу потом еще раз.
     *
     * @param category - категория товаров
     * @return кол-во страниц для этой категории
     */
    private Integer getPagesCount(Category category) {
        try {
            this.driver.get(category.link);
        } catch (WebDriverException e) {
            logger.error(String.format("Не удалось получить кол-во страниц по ссылке: %s. Пропускаем всю категорию.",
                category.link
            ));
            return null;
        }

        String source = this.driver.getPageSource();

        Document page = Jsoup.parse(source);

        Elements paginationLinks = page.selectXpath(
            "(//*[starts-with(@class, 'pagination_link')])[position() = last() - 1]");

        List<Product> products = getProductsFromPage(source);

        if (products != null) category.addProducts(products);

        if (paginationLinks.isEmpty()) return 1;

        String lastPage = paginationLinks.getFirst()
            .text();

        try {
            return Integer.parseInt(lastPage);
        } catch (NumberFormatException e) {
            logger.error(String.format("Не удалось получить кол-во страниц по ссылке: %s. Пропускаем всю категорию.",
                category.link
            ));
            return null;
        }
    }

    /**
     * Делаем все то же самое, только изначально заходит на страницу, получается ее исходный код,
     * а уже потом парсит товары
     * См. док. getProductsFromPage()
     *
     * @param link ссылка на страницу категории
     * @param page номер страницы
     * @return список товаров
     */
    private List<Product> getProductsFromPage(String link, Integer page) {
        String paginationLink = String.format("%s&page=%d", link, page);
        try {
            this.driver.get(paginationLink);
        } catch (WebDriverException e) {
            logger.error(String.format("Не удалось собрать товары по ссылке: %s", paginationLink));
            return null;
        }

        return getProductsFromPage(this.driver.getPageSource());
    }

    /**
     * Находит продукты в html-коде преобразуя их в объекты
     *
     * @param pageSource html-код страницы
     * @return список спаршенных продуктов
     */
    private List<Product> getProductsFromPage(String pageSource) {
        Document doc = Jsoup.parse(pageSource);

        Elements products = doc.selectXpath("//*[starts-with(@class, 'ProductCard_root')]/a");

        return products.stream()
            .map(product -> {
                Elements children = product.children();

                String title = children.select("h3")
                    .text();
                Double discountedPrice = extractPriceFromText(children.select("[class^=ProductCardPrice_price]")
                    .text());
                Double originalPrice = extractPriceFromText(children.select("[class^=ProductCardPrice_originalPrice]")
                    .text());
                String link = settings.getSbermarketUrl() + product.attribute("href")
                    .getValue();

                return new Product(title, link, originalPrice, discountedPrice);
            })
            .toList();
    }

    /**
     * Извлекает из строки цену стовара
     *
     * @param text текст элемента цены товара(пр. Цена за 1кг 49,99)
     * @return цена товара
     */
    private Double extractPriceFromText(String text) {
        Pattern pattern = Pattern.compile("\\d+[,.]\\d+");

        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;

        String price = matcher.group()
            .replace(",", ".");

        try {
            return Double.parseDouble(price);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    public List<List<String>> getStoreCsvRecords() {
        return stores.stream()
            .flatMap(store -> store.createCSVReport()
                .stream())
            .toList();
    }
}
