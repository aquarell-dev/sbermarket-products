package org.sbermarket.manager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sbermarket.market.Market;
import org.sbermarket.market.Store;
import org.sbermarket.settings.Settings;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarketManager {
    private final ExecutorService executorService;
    private final HashSet<Store> stores;
    private final Settings settings;
    private final List<List<String>> csvRecords = new ArrayList<>();
    private final Logger logger = LogManager.getLogger(MarketManager.class);

    public MarketManager(Settings settings) {
        this.settings = settings;
        this.stores = settings.getStores();
        this.executorService = Executors.newFixedThreadPool(settings.getNumberOfThreads());
    }

    public void start() {
        List<HashSet<Store>> batches = Store.splitIntoBatches(stores, settings.getNumberOfThreads());

        CountDownLatch latch = new CountDownLatch(batches.size());

        batches.forEach(stores -> executorService.submit(() -> {
            try {
                Market market = new Market(settings, stores);
                market.start();
                csvRecords.addAll(market.getStoreCsvRecords());
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            return;
        }

        executorService.shutdown();

        createCsvReport();
    }

    private void createCsvReport() {
        LocalDateTime now = LocalDateTime.now();

        // Format the date and time as required
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm");
        String formattedDateTime = now.format(formatter);

        Path filePath = Paths.get("output")
            .resolve(String.format("sbermarket-%s.csv", formattedDateTime));

        try (Writer writer = new FileWriter(filePath.toAbsolutePath().toFile()); CSVPrinter csvPrinter = new CSVPrinter(writer,
            CSVFormat.DEFAULT
        )) {
            csvPrinter.printRecord(Store.getCsvHeaders());

            for (List<String> rowData : csvRecords) {
                csvPrinter.printRecord(rowData);
            }

            csvPrinter.flush();

            logger.info(String.format("Файл был сохранен: %s", filePath.toAbsolutePath()));
        } catch (IOException e) {
            logger.error("Не удалось сохранить данные в файл");
        }
    }
}
