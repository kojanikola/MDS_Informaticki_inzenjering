package rs.mds.task.order.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import rs.mds.task.order.dto.CreateOrderRequest;
import rs.mds.task.order.dto.OrderResponse;
import rs.mds.task.order.exception.OrderPublishException;
import rs.mds.task.order.model.OrderState;
import rs.mds.task.order.model.OrderStatus;
import rs.mds.task.order.service.OrderService;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void validOrderReturns202() throws Exception {
        when(orderService.placeOrder(new CreateOrderRequest("123", "item-1", 2)))
                .thenReturn(OrderResponse.accepted("123"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "123", "itemId": "item-1", "quantity": 2}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value("123"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(orderService).placeOrder(new CreateOrderRequest("123", "item-1", 2));
    }

    @Test
    void invalidOrderReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": " ", "quantity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.orderId").exists())
                .andExpect(jsonPath("$.errors.itemId").exists())
                .andExpect(jsonPath("$.errors.quantity").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    void brokerFailureReturns503() throws Exception {
        when(orderService.placeOrder(new CreateOrderRequest("123", "item-1", 2)))
                .thenThrow(new OrderPublishException("broker down"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId": "123", "itemId": "item-1", "quantity": 2}
                                """))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getOrderReturnsState() throws Exception {
        when(orderService.findState("123")).thenReturn(Optional.of(
                new OrderState("123", OrderStatus.RESERVED, null, Instant.now())));

        mockMvc.perform(get("/orders/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void getUnknownOrderReturns404() throws Exception {
        when(orderService.findState("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/nope")).andExpect(status().isNotFound());
    }

    @Test
    void responseEchoesGivenCorrelationId() throws Exception {
        when(orderService.findState("123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/123").header("X-Correlation-Id", "abc-1"))
                .andExpect(header().string("X-Correlation-Id", "abc-1"));
    }

    @Test
    void responseGeneratesCorrelationIdWhenMissing() throws Exception {
        when(orderService.findState("123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/123"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }
}
