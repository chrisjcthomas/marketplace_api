package com.example.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.marketplace.domain.Business;
import com.example.marketplace.domain.CustomerOrder;
import com.example.marketplace.domain.NotificationType;
import com.example.marketplace.domain.Product;
import com.example.marketplace.domain.UserAccount;
import com.example.marketplace.dto.OrderDtos.CreateOrderRequest;
import com.example.marketplace.dto.OrderDtos.OrderItemRequest;
import com.example.marketplace.exception.BusinessRuleException;
import com.example.marketplace.repository.CustomerOrderRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OrderServiceTest {

    @Mock
    private CustomerOrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private NotificationService notificationService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderService(orderRepository, userService, productService, notificationService);
    }

    @Test
    void createCalculatesTotalReducesStockAndCreatesNotification() {
        UserAccount customer = withId(new UserAccount("Ada Lovelace", "ada@example.com"), 1L);
        Business business = withId(new Business("Clean Market", "Retail", customer), 10L);
        Product product = withId(new Product("Notebook", new BigDecimal("12.50"), 5, business), 20L);

        when(userService.requireUser(1L)).thenReturn(customer);
        when(productService.requireProduct(20L)).thenReturn(product);
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 99L));

        var response = orderService.create(new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(20L, 2))
        ));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.totalAmount()).isEqualByComparingTo("25.00");
        assertThat(product.getStockQuantity()).isEqualTo(3);

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getItems()).hasSize(1);
        verify(notificationService).create(customer, NotificationType.ORDER_CREATED, "Order 99 was created.");
    }

    @Test
    void createRejectsOrderWhenStockIsTooLow() {
        UserAccount customer = withId(new UserAccount("Grace Hopper", "grace@example.com"), 1L);
        Business business = withId(new Business("Tech Goods", "Electronics", customer), 10L);
        Product product = withId(new Product("Keyboard", new BigDecimal("50.00"), 1, business), 20L);

        when(userService.requireUser(1L)).thenReturn(customer);
        when(productService.requireProduct(20L)).thenReturn(product);

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(
                1L,
                List.of(new OrderItemRequest(20L, 2))
        ))).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");
    }

    private static <T> T withId(T target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
            return target;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
