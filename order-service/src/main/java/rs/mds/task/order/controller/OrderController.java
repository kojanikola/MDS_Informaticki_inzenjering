package rs.mds.task.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.mds.task.order.dto.CreateOrderRequest;
import rs.mds.task.order.dto.OrderResponse;
import rs.mds.task.order.service.OrderService;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Place an order",
            description = "Validates the order and publishes an event to the broker. "
                    + "Stock is reserved asynchronously by the inventory service.")
    @ApiResponse(responseCode = "202", description = "Order accepted and published")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(request);
    }
}
