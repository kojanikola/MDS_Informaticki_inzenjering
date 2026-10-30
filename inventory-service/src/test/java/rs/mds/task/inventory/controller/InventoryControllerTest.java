package rs.mds.task.inventory.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.mds.task.inventory.model.Reservation;
import rs.mds.task.inventory.model.ReservationStatus;
import rs.mds.task.inventory.service.InventoryService;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void returnsStock() throws Exception {
        when(inventoryService.currentStock()).thenReturn(Map.of("item-1", 8));

        mockMvc.perform(get("/inventory/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item-1").value(8));
    }

    @Test
    void returnsReservation() throws Exception {
        when(inventoryService.findReservation("123")).thenReturn(Optional.of(
                new Reservation("123", "item-1", 2, ReservationStatus.RESERVED, null, Instant.now())));

        mockMvc.perform(get("/inventory/reservations/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void unknownReservationReturns404() throws Exception {
        when(inventoryService.findReservation("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/inventory/reservations/nope")).andExpect(status().isNotFound());
    }
}
