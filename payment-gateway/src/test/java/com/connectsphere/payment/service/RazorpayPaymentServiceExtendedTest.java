package com.connectsphere.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.connectsphere.payment.dto.CreateRazorpayOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Additional branch coverage for RazorpayPaymentService –
 * covers the InterruptedException path in createOrder.
 */
class RazorpayPaymentServiceExtendedTest {

    // -------------------------------------------------------------------------
    // createOrder – InterruptedException path
    // -------------------------------------------------------------------------

    @Test
    void createOrderWrapsInterruptedException() throws Exception {
        HttpClient client = mock(HttpClient.class);
        RazorpayPaymentService service = new RazorpayPaymentService(new ObjectMapper(), client);
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
        when(client.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("simulated interrupt"));

        CreateRazorpayOrderRequest request = new CreateRazorpayOrderRequest(5000, "receipt-interrupt");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createOrder(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Could not create Razorpay order", ex.getReason());
        // Thread's interrupted flag should have been restored
        assertTrue(Thread.currentThread().isInterrupted());
        // Clear so we don't pollute other tests
        Thread.interrupted();
    }

    // -------------------------------------------------------------------------
    // createOrder – IOException path
    // -------------------------------------------------------------------------

    @Test
    void createOrderWrapsIOException() throws Exception {
        HttpClient client = mock(HttpClient.class);
        RazorpayPaymentService service = new RazorpayPaymentService(new ObjectMapper(), client);
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
        when(client.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("connection refused"));

        CreateRazorpayOrderRequest ioRequest = new CreateRazorpayOrderRequest(5000, "receipt-io");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createOrder(ioRequest));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // verifyPayment – unconfigured keys
    // -------------------------------------------------------------------------

    @Test
    void verifyPaymentThrowsWhenKeysNotConfigured() {
        RazorpayPaymentService service = new RazorpayPaymentService(new ObjectMapper());
        // Keys are blank by default → assertRazorpayConfigured throws
        assertThrows(ResponseStatusException.class,
                () -> service.verifyPayment(
                        new com.connectsphere.payment.dto.VerifyRazorpayPaymentRequest(
                                "order_1", "pay_1", "sig")));
    }
}
