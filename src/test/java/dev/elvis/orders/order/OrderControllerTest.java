package dev.elvis.orders.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrderService service;

    @Test
    void createsOrder() throws Exception {
        var order = Order.create("customer-1", new BigDecimal("10.00"), Instant.now());
        when(service.create("customer-1", new BigDecimal("10.00"))).thenReturn(order);

        mvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"customerId": "customer-1", "amount": 10.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/orders/" + order.getId()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void rejectsNonPositiveAmount() throws Exception {
        mvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"customerId": "customer-1", "amount": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        var id = UUID.randomUUID();
        when(service.find(id)).thenThrow(new OrderNotFoundException(id));

        mvc.perform(get("/orders/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void returnsConflictForInvalidTransition() throws Exception {
        var id = UUID.randomUUID();
        when(service.pay(any()))
                .thenThrow(new InvalidOrderTransitionException(id, OrderStatus.CANCELLED, OrderStatus.PAID));

        mvc.perform(post("/orders/{id}/pay", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Order " + id + " cannot go from CANCELLED to PAID"));
    }
}
