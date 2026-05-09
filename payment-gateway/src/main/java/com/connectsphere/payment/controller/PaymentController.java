package com.connectsphere.payment.controller;

import com.connectsphere.payment.dto.CreateRazorpayOrderRequest;
import com.connectsphere.payment.dto.CreateRazorpayOrderResponse;
import com.connectsphere.payment.dto.VerifyRazorpayPaymentRequest;
import com.connectsphere.payment.dto.VerifyRazorpayPaymentResponse;
import com.connectsphere.payment.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Exposes Payment API endpoints.
 */


@RestController
@RequiredArgsConstructor
@RequestMapping("/payments/razorpay")

public class PaymentController {

    private final RazorpayPaymentService razorpayPaymentService;
/**
 * Creates order.
 * @param request request payload
 * @return operation result
 */

    @PostMapping("/orders")
    public CreateRazorpayOrderResponse createOrder(@Valid @RequestBody CreateRazorpayOrderRequest request) {
        return razorpayPaymentService.createOrder(request);
    }
/**
 * Handles the payment request.
 * @param request request payload
 * @return operation result
 */

    @PostMapping("/verify")
    public VerifyRazorpayPaymentResponse verifyPayment(@Valid @RequestBody VerifyRazorpayPaymentRequest request) {
        return razorpayPaymentService.verifyPayment(request);
    }
}
