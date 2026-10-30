package rs.mds.task.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.mds.task.inventory.model.Reservation;
import rs.mds.task.inventory.service.InventoryService;

/**
 * Read-only endpoints to inspect the in-memory state; orders themselves arrive through the broker.
 */
@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stock")
    @Operation(summary = "Current available quantity per item")
    public Map<String, Integer> stock() {
        return inventoryService.currentStock();
    }

    @GetMapping("/reservations/{orderId}")
    @Operation(summary = "Processing result of an order")
    @ApiResponse(responseCode = "200", description = "Order was processed (reserved or rejected)")
    @ApiResponse(responseCode = "404", description = "Order not processed (yet)")
    public ResponseEntity<Reservation> reservation(@PathVariable String orderId) {
        return inventoryService.findReservation(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
