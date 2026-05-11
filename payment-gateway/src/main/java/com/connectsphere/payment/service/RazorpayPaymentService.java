package com.connectsphere.payment.service;

import com.connectsphere.payment.dto.CreateRazorpayOrderRequest;
import com.connectsphere.payment.dto.CreateRazorpayOrderResponse;
import com.connectsphere.payment.dto.VerifyRazorpayPaymentRequest;
import com.connectsphere.payment.dto.VerifyRazorpayPaymentResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
/**
 * Provides Razorpay Payment business operations.
 */


@Service
public class RazorpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Autowired
    public RazorpayPaymentService(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newHttpClient());
    }

    RazorpayPaymentService(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

/**
 * Creates order.
 * @param request request payload
 * @return operation result
 */
    public CreateRazorpayOrderResponse createOrder(CreateRazorpayOrderRequest request) {
        assertRazorpayConfigured();

        try {
            Map<String, Object> payload = Map.of(
                    "amount", request.amountPaise(),
                    "currency", "INR",
                    "receipt", request.receipt()
            );
            String credentials = Base64.getEncoder()
                    .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest razorpayRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header("Authorization", "Basic " + credentials)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(razorpayRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                int statusCode = response.statusCode();
                String responseBody = response.body();
                log.warn("Razorpay order creation failed: status={}, body={}", statusCode, responseBody);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not create Razorpay order");
            }

            Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
            Object id = body.get("id");
            if (id == null || id.toString().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order response did not include order id");
            }

            return new CreateRazorpayOrderResponse(razorpayKeyId, id.toString(), request.amountPaise(), "INR");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Razorpay order creation was interrupted for receipt={}", request.receipt(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not create Razorpay order");
        } catch (java.io.IOException | RuntimeException ex) {
            log.warn("Could not create Razorpay order for receipt={}", request.receipt(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not create Razorpay order");
        }
    }

/**
 * Verifies payment.
 * @param request request payload
 * @return operation result
 */
    public VerifyRazorpayPaymentResponse verifyPayment(VerifyRazorpayPaymentRequest request) {
        assertRazorpayConfigured();
        return new VerifyRazorpayPaymentResponse(isValidRazorpaySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature()
        ));
    }

    private void assertRazorpayConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
    }

    private boolean isValidRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest).equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.warn("Could not verify Razorpay signature", ex);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
