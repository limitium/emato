package org.example;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Properties;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "http://localhost:3000") // Vite's default port
public class EmailParserApplication {

  @Value("${ai.lab.maildrop.url}")
  private String mailDropUrl;

  public static void main(String[] args) {
    SpringApplication.run(EmailParserApplication.class, args);
  }

  // Mock data storage
  private final List<Email> emails = new ArrayList<>();
  private long nextId = 1;
  private final Random random = new Random();

  // Analytics endpoint
  @GetMapping("/api/analytics")
  public ResponseEntity<Analytics> getAnalytics() {
    Analytics analytics =
        new Analytics(
            1000, // totalEmails
            850, // totalExtracted
            720, // totalSent
            680 // totalUntouched
            );
    return ResponseEntity.ok(analytics);
  }

  // Get emails with pagination and filtering
  @GetMapping("/api/emails")
  public ResponseEntity<List<Email>> getEmails() {
    // Initialize with mock data if empty
    if (emails.isEmpty()) {
      initializeMockData();
    }

    // Return all emails sorted by creation date (newest first)
    List<Email> sortedEmails =
        emails.stream()
            .sorted((e1, e2) -> e2.createdAt().compareTo(e1.createdAt()))
            .collect(Collectors.toList());

    return ResponseEntity.ok(sortedEmails);
  }

  // Parse new email
  @PostMapping("/api/parse")
  public ResponseEntity<Email> parseEmail(@RequestBody EmailRequest request) {
    try {
      // Create mail session and parse email
      Session session = Session.getDefaultInstance(new Properties());
      MimeMessage mimeMessage =
          new MimeMessage(session, new ByteArrayInputStream(request.body().getBytes()));

      // Extract email metadata
      String fromEmail = "Unknown";
      if (mimeMessage.getFrom() != null && mimeMessage.getFrom().length > 0) {
        InternetAddress address = (InternetAddress) mimeMessage.getFrom()[0];
        fromEmail = address.getAddress();
      }

      String subject = mimeMessage.getSubject();
      if (subject == null) {
        subject = "No Subject";
      }

      // Extract recipients
      String[] toAddresses =
          mimeMessage.getRecipients(Message.RecipientType.TO) != null
              ? Arrays.stream(mimeMessage.getRecipients(Message.RecipientType.TO))
                  .map(address -> ((InternetAddress) address).getAddress())
                  .toArray(String[]::new)
              : new String[0];

      String[] ccAddresses =
          mimeMessage.getRecipients(Message.RecipientType.CC) != null
              ? Arrays.stream(mimeMessage.getRecipients(Message.RecipientType.CC))
                  .map(address -> ((InternetAddress) address).getAddress())
                  .toArray(String[]::new)
              : new String[0];

      // Get content
      String body = extractEmailBody(mimeMessage);

      // Create WebClient for making HTTP requests
      WebClient webClient = WebClient.create();

      // Create and send AI request
      MailDropRequest mailDropRequest =
          new MailDropRequest(
              "aggregated_email_model",
              UUID.randomUUID().toString(),
              fromEmail,
              toAddresses,
              ccAddresses,
              subject,
              body,
              LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

      // Send request to AI Lab endpoint with error handling
      MailDropResponse aiResponse;
      try {
        aiResponse =
            webClient
                .post()
                .uri(mailDropUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mailDropRequest)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response ->
                        response
                            .bodyToMono(String.class)
                            .flatMap(
                                errorBody ->
                                    Mono.error(
                                        new RuntimeException(
                                            "AI server error: "
                                                + response.statusCode()
                                                + " - "
                                                + errorBody))))
                .bodyToMono(MailDropResponse.class)
                .block();
      } catch (Exception e) {
        throw new RuntimeException("AI server error: " + e.getMessage(), e);
      }

      // Convert quotes to trades
      List<Trade> trades = new ArrayList<>();
      if (aiResponse != null && aiResponse.quotes() != null) {
        for (Quote quote : aiResponse.quotes()) {
          trades.add(convertQuoteToTrade(quote, nextId));
        }
      }

      // Create Email object
      Email email =
          new Email(
              nextId++,
              subject,
              fromEmail,
              toAddresses,
              ccAddresses,
              body,
              trades,
              false,
              LocalDateTime.now(),
              LocalDateTime.now());

      emails.add(email);
      return ResponseEntity.ok(email);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .header("X-Error-Message", e.getMessage())
          .body(null);
    }
  }

  // Helper method to extract email body
  private String extractEmailBody(MimeMessage mimeMessage) throws Exception {
    Object content = mimeMessage.getContent();
    if (content instanceof String) {
      return (String) content;
    } else if (content instanceof Multipart) {
      Multipart multipart = (Multipart) content;
      StringBuilder body = new StringBuilder();
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart bodyPart = multipart.getBodyPart(i);
        String partContentType = bodyPart.getContentType().toLowerCase();
        if (partContentType.contains("html")) {
          return bodyPart.getContent().toString();
        } else if (partContentType.contains("text/plain")) {
          body.append(bodyPart.getContent().toString());
        }
      }
      return body.toString();
    }
    return content.toString();
  }

  // Helper method to parse date strings
  private Date parseDate(String dateStr) {
    try {
      return Date.from(LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant());
    } catch (Exception e) {
      return new Date();
    }
  }

  // Update email
  @PutMapping("/api/email/{id}")
  public ResponseEntity<Email> updateEmail(@PathVariable Long id, @RequestBody Email updatedEmail) {
    // Initialize with mock data if empty
    initializeMockData();

    // Find the existing email by ID
    for (int i = 0; i < emails.size(); i++) {
      Email existingEmail = emails.get(i);
      if (existingEmail.id().equals(id)) {
        // Merge non-null fields from updatedEmail into existingEmail
        Email mergedEmail =
            new Email(
                existingEmail.id(),
                updatedEmail.subject() != null ? updatedEmail.subject() : existingEmail.subject(),
                updatedEmail.fromEmail() != null
                    ? updatedEmail.fromEmail()
                    : existingEmail.fromEmail(),
                updatedEmail.toEmails() != null
                    ? updatedEmail.toEmails()
                    : existingEmail.toEmails(),
                updatedEmail.cc() != null ? updatedEmail.cc() : existingEmail.cc(),
                updatedEmail.body() != null ? updatedEmail.body() : existingEmail.body(),
                updatedEmail.trades() != null ? updatedEmail.trades() : existingEmail.trades(),
                updatedEmail.sent() != null ? updatedEmail.sent() : existingEmail.sent(),
                existingEmail.createdAt(),
                LocalDateTime.now() // Update modifiedAt timestamp
                );

        // Update the email in the list
        emails.set(i, mergedEmail);
        return ResponseEntity.ok(mergedEmail);
      }
    }
    return ResponseEntity.notFound().build();
  }

  // Get single email
  @GetMapping("/api/email/{id}")
  public ResponseEntity<Email> getEmail(@PathVariable Long id) {
    // Initialize with mock data if empty
    if (emails.isEmpty()) {
      initializeMockData();
    }

    return emails.stream()
        .filter(e -> e.id().equals(id))
        .findFirst()
        .map(ResponseEntity::ok)
        .orElseGet(
            () -> {
              // If email not found, create a mock one
              LocalDateTime now = LocalDateTime.now();
              List<Trade> mockTrades =
                  Arrays.asList(
                      new Trade(
                          nextTradeId++,
                          id,
                          true, // isSuccess
                          null, // errorMessage
                          "BUY", // clientWay
                          "USD", // currency
                          "US0378331005", // isinCode
                          "TRADE_001", // schemaIdentifier
                          "EQUITY", // schemaType
                          "1.0", // schemaVersion
                          "HEADER_001", // solveHeader
                          "CLIENT_001", // clientAlias
                          "BROKER_001", // brokerAlias
                          1000.0, // quantity
                          150.0, // price
                          LocalDate.now().format(DateTimeFormatter.ISO_DATE), // tradeDate
                          LocalDate.now()
                              .plusDays(2)
                              .format(DateTimeFormatter.ISO_DATE), // settlementDate
                          now // createdAt
                          ));

              Email mockEmail =
                  new Email(
                      id,
                      "Trade Confirmation",
                      "trader@bank.com",
                      new String[] {"ops@bank.com"},
                      new String[] {"compliance@bank.com"},
                      "Mock email body content",
                      mockTrades,
                      false,
                      now,
                      now);

              emails.add(mockEmail);
              return ResponseEntity.ok(mockEmail);
            });
  }

  // Helper method to generate mock data
  private void initializeMockData() {
    if (emails.isEmpty()) {
      Random random = new Random();
      String[] subjects = {
        "Trade Confirmation - AAPL",
        "FWD: Trade Details - MSFT",
        "RE: Trading Instructions",
        "New Trade Alert",
        "Trade Settlement Notice"
      };
      String[] fromEmails = {
        "trader1@bank.com",
        "trader2@bank.com",
        "ops@bank.com",
        "settlement@bank.com",
        "trading.desk@bank.com"
      };
      String[] isinCodes = {
        "US0378331005", // AAPL
        "US5949181045", // MSFT
        "US02079K1079", // GOOGL
        "US88160R1014", // TSLA
        "US0231351067" // AMZN
      };
      String[] currencies = {"USD", "EUR", "GBP", "JPY", "CHF"};
      String[] clientWays = {"BUY", "SELL"};

      // Generate 50 mock emails
      for (int i = 0; i < 50; i++) {
        LocalDateTime emailDate = LocalDateTime.now().minusDays(random.nextInt(30));

        // Generate 1-3 trades per email
        List<Trade> trades = new ArrayList<>();
        int numTrades = random.nextInt(3) + 1;

        for (int j = 0; j < numTrades; j++) {
          boolean isSuccess = random.nextDouble() > 0.2; // 80% success rate
          trades.add(
              new Trade(
                  nextTradeId++,
                  (long) i + 1,
                  isSuccess,
                  isSuccess
                      ? null
                      : "Failed to process trade: Invalid "
                          + (random.nextBoolean() ? "price" : "quantity"),
                  clientWays[random.nextInt(clientWays.length)],
                  currencies[random.nextInt(currencies.length)],
                  isinCodes[random.nextInt(isinCodes.length)],
                  "SCHEMA_" + (i + 1),
                  "EQUITY",
                  "1.0",
                  "HEADER_" + (i + 1),
                  "CLIENT_" + (random.nextInt(5) + 1),
                  "BROKER_" + (random.nextInt(5) + 1),
                  100.0 * (random.nextInt(10) + 1), // quantity between 100-1000
                  50.0 + random.nextDouble() * 950.0, // price between 50-1000
                  emailDate.format(DateTimeFormatter.ISO_DATE),
                  emailDate.plusDays(2).format(DateTimeFormatter.ISO_DATE),
                  emailDate));
        }

        emails.add(
            new Email(
                (long) i + 1,
                subjects[random.nextInt(subjects.length)] + " #" + (i + 1),
                fromEmails[random.nextInt(fromEmails.length)],
                new String[] {"recipient@bank.com"},
                new String[] {"compliance@bank.com"},
                "Mock email body content for email #" + (i + 1),
                trades,
                random.nextBoolean(),
                emailDate,
                emailDate));
      }
    }
  }

  // Add these records at the bottom of the file with other records
  record MailDropRequest(
      String model,
      String uniqueId,
      String from,
      String[] to,
      String[] cc,
      String subject,
      String htmlPart,
      String timeReceived) {}

  record MailDropResponse(List<Quote> quotes) {}

  record Quote(Check check, Contract contract) {}

  record Check(
      Boolean isSuccess,
      List<Error> errors,
      String message,
      String messageToDisplay,
      String type) {}

  record Error(Integer code, List<Field> fields) {}

  record Field(String initialName, String modelName) {}

  record Contract(
      String clientWay,
      String currency,
      String isinCode,
      String schemaIdentifier,
      String schemaType,
      String schemaVersion,
      String solveHeader) {}

  // Add a counter for trade IDs
  private static Long nextTradeId = 1L;

  // Add this mock endpoint
  @PostMapping("/ai-lab/maildrop")
  public ResponseEntity<MailDropResponse> mockAiLabMaildrop(@RequestBody MailDropRequest request) {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Random random = new Random();
    List<Quote> quotes = new ArrayList<>();

    int numQuotes = random.nextInt(3) + 1;
    for (int i = 0; i < numQuotes; i++) {
      boolean isSuccess = random.nextDouble() > 0.2;

      Check check;
      if (isSuccess) {
        check = new Check(true, null, null, null, null);
      } else {
        check =
            new Check(
                false,
                List.of(
                    new Error(
                        10100,
                        List.of(new Field("output_json_clientWay", "output_json_clientWay")))),
                "Failed to predict the output fields",
                "Failed to predict the output fields",
                "Solving error");
      }

      Contract contract =
          new Contract(
              isSuccess ? (random.nextBoolean() ? "Buy" : "Sell") : "",
              isSuccess ? (random.nextBoolean() ? "USD" : "EUR") : "",
              isSuccess ? "JE00B4T3BW64" : "",
              "", // schemaIdentifier
              "", // schemaType
              "", // schemaVersion
              "" // solveHeader
              );

      quotes.add(new Quote(check, contract));
    }

    return ResponseEntity.ok(new MailDropResponse(quotes));
  }

  private Trade convertQuoteToTrade(Quote quote, Long emailId) {
    return new Trade(
        nextTradeId++,
        emailId,
        quote.check().isSuccess(),
        !quote.check().isSuccess() ? quote.check().messageToDisplay() : null,
        quote.contract().clientWay(),
        quote.contract().currency(),
        quote.contract().isinCode(),
        quote.contract().schemaIdentifier(),
        quote.contract().schemaType(),
        quote.contract().schemaVersion(),
        quote.contract().solveHeader(),
        "CLIENT_" + emailId,
        "BROKER_" + emailId,
        1000.0, // default quantity
        100.0, // default price
        LocalDate.now().format(DateTimeFormatter.ISO_DATE),
        LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_DATE),
        LocalDateTime.now());
  }
}

// Data classes
record Analytics(int totalEmails, int totalExtracted, int totalSent, int totalUntouched) {}

record Email(
    // Email metadata
    Long id,
    String subject,
    String fromEmail,
    String[] toEmails,
    String[] cc,
    String body,
    List<Trade> trades,
    Boolean sent,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {}

record Trade(
    Long id,
    Long emailId,
    Boolean isSuccess,
    String errorMessage,
    // AI extracted fields
    String clientWay,
    String currency,
    String isinCode,
    String schemaIdentifier,
    String schemaType,
    String schemaVersion,
    String solveHeader,
    // Additional trade details
    String clientAlias,
    String brokerAlias,
    Double quantity,
    Double price,
    String tradeDate,
    String settlementDate,
    LocalDateTime createdAt) {}

record EmailRequest(String body) {}

record EmailResponse(List<Email> items, int total) {}
