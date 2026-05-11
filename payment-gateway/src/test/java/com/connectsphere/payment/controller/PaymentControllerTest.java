package com.connectsphere.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connectsphere.payment.dto.CreateRazorpayOrderRequest;
import com.connectsphere.payment.dto.CreateRazorpayOrderResponse;
import com.connectsphere.payment.dto.VerifyRazorpayPaymentRequest;
import com.connectsphere.payment.dto.VerifyRazorpayPaymentResponse;
import com.connectsphere.payment.service.RazorpayPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller-layer tests for PaymentController targeting ≥80% coverage.
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private RazorpayPaymentService razorpayPaymentService;

    // ---- POST /payments/razorpay/orders ------------------------------------

    @Test
    void createOrderDelegatesToServiceAndReturnsResponse() throws Exception {
        CreateRazorpayOrderRequest request = new CreateRazorpayOrderRequest(5000, "receipt-test");
        CreateRazorpayOrderResponse response =
                new CreateRazorpayOrderResponse("rzp_key", "order_abc", 5000, "INR");
        when(razorpayPaymentService.createOrder(any(CreateRazorpayOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/payments/razorpay/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_abc"))
                .andExpect(jsonPath("$.keyId").value("rzp_key"))
                .andExpect(jsonPath("$.amountPaise").value(5000))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    // ---- POST /payments/razorpay/verify ------------------------------------

    @Test
    void verifyPaymentReturnsTrueForValidSignature() throws Exception {
        VerifyRazorpayPaymentRequest request =
                new VerifyRazorpayPaymentRequest("order_1", "pay_1", "sig");
        when(razorpayPaymentService.verifyPayment(any(VerifyRazorpayPaymentRequest.class)))
                .thenReturn(new VerifyRazorpayPaymentResponse(true));

        mockMvc.perform(post("/payments/razorpay/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void verifyPaymentReturnsFalseForInvalidSignature() throws Exception {
        VerifyRazorpayPaymentRequest request =
                new VerifyRazorpayPaymentRequest("order_1", "pay_1", "bad-sig");
        when(razorpayPaymentService.verifyPayment(any(VerifyRazorpayPaymentRequest.class)))
                .thenReturn(new VerifyRazorpayPaymentResponse(false));

        mockMvc.perform(post("/payments/razorpay/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
