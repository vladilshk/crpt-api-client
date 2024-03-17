import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class CrptApi {
    private final HttpClient httpClient;
    private final BlockingQueue<Long> requestTimes;
    private final int requestLimit;
    private final Duration interval;
    private final Gson gson;
    private final Object lock = new Object();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.requestTimes = new LinkedBlockingQueue<>(requestLimit);
        this.requestLimit = requestLimit;
        this.interval = Duration.ofMillis(timeUnit.toMillis(1));
        this.gson = new Gson();
    }

    public void createDocument(Document document, String signature) throws InterruptedException {
        checkRequestLimit();

        String requestBody = gson.toJson(document);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response: " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send HTTP request", e);
        }
    }

    private void checkRequestLimit() throws InterruptedException {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            while (!requestTimes.isEmpty() && currentTime - requestTimes.peek() > interval.toMillis()) {
                requestTimes.take();
            }
            if (requestTimes.size() >= requestLimit) {
                long sleepTime = interval.toMillis() - (currentTime - requestTimes.peek());
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                requestTimes.clear();
            }
            requestTimes.add(currentTime);
        }
    }

    // Internal class representing the document
    public static class Document {
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private String reg_date;
        private String reg_number;
        private Description description;
        private Product[] products;

        // Constructor, getters and setters
    }

    public static class Description {
        private String participantInn;

        // Constructor, getters and setters
    }

    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        // Constructor, getters and setters
    }

    // Example main method to demonstrate usage
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10);
        CrptApi.Document document = new CrptApi.Document();
        // Populate document fields

        Runnable task = () -> {
            try {
                for (int i = 0; i < 2; i++) {
                    crptApi.createDocument(document, "signature");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        for (int i = 0; i < 10; i++) {
            new Thread(task).start();
        }
    }
}