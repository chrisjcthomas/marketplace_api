package com.example.marketplace.service;

import com.example.marketplace.domain.CustomerOrder;
import com.example.marketplace.domain.NotificationType;
import com.example.marketplace.domain.OrderItem;
import com.example.marketplace.domain.Product;
import com.example.marketplace.domain.UserAccount;
import com.example.marketplace.dto.OrderDtos.CreateOrderRequest;
import com.example.marketplace.dto.OrderDtos.OrderItemResponse;
import com.example.marketplace.dto.OrderDtos.OrderResponse;
import com.example.marketplace.dto.OrderDtos.UpdateOrderStatusRequest;
import com.example.marketplace.exception.BusinessRuleException;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final UserService userService;
    private final ProductService productService;
    private final NotificationService notificationService;

    public OrderService(
            CustomerOrderRepository orderRepository,
            UserService userService,
            ProductService productService,
            NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.productService = productService;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        UserAccount customer = userService.requireUser(request.customerUserId());
        CustomerOrder order = new CustomerOrder(customer);

        request.items().forEach(item -> {
            Product product = productService.requireProduct(item.productId());
            if (product.getStockQuantity() < item.quantity()) {
                throw new BusinessRuleException("Insufficient stock for product " + product.getId());
            }
            product.reduceStock(item.quantity());
            order.addItem(product, item.quantity());
        });

        CustomerOrder saved = orderRepository.save(order);
        notificationService.create(
                customer,
                NotificationType.ORDER_CREATED,
                "Order " + saved.getId() + " was created."
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return toResponse(requireOrder(id));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        CustomerOrder order = requireOrder(id);
        order.updateStatus(request.status());
        notificationService.create(
                order.getCustomer(),
                NotificationType.ORDER_STATUS_UPDATED,
                "Order " + order.getId() + " status changed to " + request.status() + "."
        );
        return toResponse(order);
    }

    CustomerOrder requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " was not found"));
    }

    static OrderResponse toResponse(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderService::toItemResponse).toList()
        );
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
