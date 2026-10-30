package rs.mds.task.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to place a new order")
public record CreateOrderRequest(
        @Schema(example = "123") @NotBlank String orderId,
        @Schema(example = "item-1") @NotBlank String itemId,
        @Schema(example = "2") @NotNull @Positive Integer quantity) {
}
