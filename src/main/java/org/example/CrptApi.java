package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    public final Semaphore semaphore;
    public final HttpClient httpClient;
    public final ObjectMapper mapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        httpClient = HttpClient.newHttpClient();
        mapper = new ObjectMapper().
                setPropertyNamingStrategy(new PropertyNamingStrategies.KebabCaseStrategy())
                .registerModule(new JavaTimeModule());

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    timeUnit.sleep(1);
                    semaphore.release(requestLimit - semaphore.availablePermits());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void createDocument(Document document, String signature)  throws IOException, InterruptedException {
        semaphore.acquire();
        try {
            String jsonDocument = mapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            synchronized (this) {
                System.out.println("Response status code: " + response.statusCode());
                System.out.println("Response body: " + response.body());
            }
        } finally {
            semaphore.release();
        }
    }

    public static void main(String[] args) {
        Product product = new Product("certificate1", LocalDate.of(2020, 1, 23),
                "certNumber123", "ownerInn123",
                "procedureInn123", LocalDate.of(2020, 1, 23),
                "tnvedCode1", "uitCode1", "uituCode1");

        Document document = new Document(new Document.Description("participantInn123"),
                "docId123", "status1", "LP_INTRODUCE_GOODS", true,
                "onwerInn123", "participantInn123", "producerInn123",
                LocalDate.of(2020, 1, 23), "productionType1",
                new ArrayList<>(List.of(product)), LocalDate.of(2020, 1, 23),
                "regNumber123");

        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            final int requestNumber = i + 1;
            new Thread(() -> {
                try {
                    System.out.println("Отправка запроса #" + requestNumber + " в " + (System.currentTimeMillis() - startTime) + "ms");
                    crptApi.createDocument(document, "test");
                    System.out.println("Запрос #" + requestNumber + " выполнен в " + (System.currentTimeMillis() - startTime) + "ms");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }

    static class Document {
        Description description;
        String docId;
        String docStatus;
        String docType;
        boolean importRequest;
        String ownerInn;
        String participantInn;
        String producerInn;
        LocalDate productionDate;
        String productionType;
        List<Product> products;
        LocalDate regDate;
        String regNumber;

        public Document(Description description,
                        String docId, String docStatus,
                        String docType, boolean importRequest,
                        String ownerInn, String participantInn,
                        String producerInn, LocalDate productionDate,
                        String productionType, List<Product> products,
                        LocalDate regDate, String regNumber) {
            this.description = description;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public LocalDate getRegDate() {
            return regDate;
        }

        public void setRegDate(LocalDate regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }

        static class Description {
            String participantInn;

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }

            public String getParticipantInn() {
                return participantInn;
            }

            public void setParticipantInn(String participantInn) {
                this.participantInn = participantInn;
            }
        }

    }

    static class Product {
        String certificateDocument;
        LocalDate certificateDocumentDate;
        String certificateDocumentNumber;
        String ownerInn;
        String producerInn;
        LocalDate productionDate;
        String tnvedCode;
        String uitCode;
        String uituCode;

        public Product(String certificateDocument, LocalDate certificateDocumentDate,
                       String certificateDocumentNumber, String ownerInn,
                       String producerInn, LocalDate productionDate,
                       String tnvedCode, String uitCode, String uituCode) {
            this.certificateDocument = certificateDocument;
            this.certificateDocumentDate = certificateDocumentDate;
            this.certificateDocumentNumber = certificateDocumentNumber;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.tnvedCode = tnvedCode;
            this.uitCode = uitCode;
            this.uituCode = uituCode;
        }

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public LocalDate getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(LocalDate certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }
    }
}
