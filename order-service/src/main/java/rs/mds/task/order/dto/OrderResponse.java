package rs.mds.task.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Acknowledgement that the order was accepted and handed to the broker")
public record OrderResponse(
        @Schema(example = "123") String orderId,
        @Schema(example = "ACCEPTED") String status) {

    public static OrderResponse accepted(String orderId) {
        return new OrderResponse(orderId, "ACCEPTED");
    }
}
