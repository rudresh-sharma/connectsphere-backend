package com.connectsphere.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.connectsphere.payment.dto.CreateRazorpayOrderRequest;
import com.connectsphere.payment.dto.VerifyRazorpayPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class RazorpayPaymentServiceTest {

    private RazorpayPaymentService service;

    @BeforeEach
    void setUp() {
        service = new RazorpayPaymentService(new ObjectMapper());
    }

    @Test
    void createOrderRequiresConfiguredKeys() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createOrder(new CreateRazorpayOrderRequest(5000, "receipt-1")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createOrderReturnsMappedResponseForSuccessfulRazorpayCall() throws Exception {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"id\":\"order_123\"}");

        try (MockedStatic<HttpClient> mocked = mockStatic(HttpClient.class)) {
            mocked.when(HttpClient::newHttpClient).thenReturn(client);
            when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

            var result = service.createOrder(new CreateRazorpayOrderRequest(5000, "receipt-1"));

            assertEquals("rzp_test_key", result.keyId());
            assertEquals("order_123", result.orderId());
            assertEquals(5000, result.amountPaise());
            assertEquals("INR", result.currency());
        }
    }

    @Test
    void createOrderRejectsNonSuccessStatus() throws Exception {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn("{\"error\":\"bad request\"}");

        try (MockedStatic<HttpClient> mocked = mockStatic(HttpClient.class)) {
            mocked.when(HttpClient::newHttpClient).thenReturn(client);
            when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> service.createOrder(new CreateRazorpayOrderRequest(5000, "receipt-1")));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Could not create Razorpay order", exception.getReason());
        }
    }

    @Test
    void createOrderRejectsMissingOrderIdInResponse() throws Exception {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"entity\":\"order\"}");

        try (MockedStatic<HttpClient> mocked = mockStatic(HttpClient.class)) {
            mocked.when(HttpClient::newHttpClient).thenReturn(client);
            when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> service.createOrder(new CreateRazorpayOrderRequest(5000, "receipt-1")));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Razorpay order response did not include order id", exception.getReason());
        }
    }

    @Test
    void createOrderWrapsUnexpectedClientFailure() throws Exception {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
        HttpClient client = mock(HttpClient.class);

        try (MockedStatic<HttpClient> mocked = mockStatic(HttpClient.class)) {
            mocked.when(HttpClient::newHttpClient).thenReturn(client);
            when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenThrow(new RuntimeException("network down"));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> service.createOrder(new CreateRazorpayOrderRequest(5000, "receipt-1")));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Could not create Razorpay order", exception.getReason());
        }
    }

    @Test
    void verifyPaymentReturnsTrueForValidSignature() throws Exception {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");

        String signature = hmac("order_1|pay_1", "secret");

        assertTrue(service.verifyPayment(new VerifyRazorpayPaymentRequest("order_1", "pay_1", signature)).valid());
    }

    @Test
    void verifyPaymentReturnsFalseForInvalidSignature() {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");

        assertFalse(service.verifyPayment(new VerifyRazorpayPaymentRequest("order_1", "pay_1", "wrong-signature")).valid());
    }

    @Test
    void verifyPaymentRequiresConfiguredKeys() {
        assertThrows(ResponseStatusException.class, () ->
                service.verifyPayment(new VerifyRazorpayPaymentRequest("order_1", "pay_1", "signature")));
    }

    private String hmac(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte current : digest) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
