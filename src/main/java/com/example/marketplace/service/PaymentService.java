package com.example.marketplace.service;

import com.example.marketplace.domain.CustomerOrder;
import com.example.marketplace.domain.NotificationType;
import com.example.marketplace.domain.OrderStatus;
import com.example.marketplace.domain.Payment;
import com.example.marketplace.domain.PaymentStatus;
import com.example.marketplace.dto.PaymentDtos.PaymentResponse;
import com.example.marketplace.dto.PaymentDtos.SimulatePaymentRequest;
import com.example.marketplace.exception.BusinessRuleException;
import com.example.marketplace.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderService orderService,
            NotificationService notificationService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @Transactional
    public PaymentResponse simulate(SimulatePaymentRequest request) {
        CustomerOrder order = orderService.requireOrder(request.orderId());
        paymentRepository.findByOrderId(order.getId()).ifPresent(existing -> {
            throw new BusinessRuleException("Order " + order.getId() + " already has a payment");
        });

        PaymentStatus paymentStatus = request.approved() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED;
        Payment saved = paymentRepository.save(new Payment(order, paymentStatus));

        if (paymentStatus == PaymentStatus.COMPLETED) {
            order.updateStatus(OrderStatus.PAID);
            notificationService.create(
                    order.getCustomer(),
                    NotificationType.PAYMENT_COMPLETED,
                    "Payment for order " + order.getId() + " was completed."
            );
        } else {
            notificationService.create(
                    order.getCustomer(),
                    NotificationType.PAYMENT_FAILED,
                    "Payment for order " + order.getId() + " failed."
            );
        }

        return toResponse(saved);
    }

    static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getSimulatedAt()
        );
    }
}
