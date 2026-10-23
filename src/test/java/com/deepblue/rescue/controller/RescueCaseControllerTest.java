package com.deepblue.rescue.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService service;

    @Test
    void shouldReturnRescueCaseByCode() throws Exception {
        RescueCaseResponse response = rescueCase(RescueStatus.IN_REHABILITATION);
        when(service.findByCode("RES-2026-001")).thenReturn(response);

        mockMvc.perform(get("/api/rescue-cases/{caseCode}", "RES-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-2026-001"))
                .andExpect(jsonPath("$.status").value("IN_REHABILITATION"));

        verify(service).findByCode("RES-2026-001");
    }

    @Test
    void shouldReturn404WhenCaseDoesNotExist() throws Exception {
        when(service.findByCode("RES-999"))
                .thenThrow(new ResourceNotFoundException("Rescue case not found: RES-999"));

        mockMvc.perform(get("/api/rescue-cases/{caseCode}", "RES-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rescue case not found: RES-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturnCasesByStatus() throws Exception {
        RescueCaseResponse first = rescueCase(RescueStatus.IN_REHABILITATION);
        RescueCaseResponse second = new RescueCaseResponse(
                2L,
                "RES-2026-002",
                LocalDate.of(2026, 8, 21),
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION,
                "DB-CAR",
                "AN-2026-002"
        );
        when(service.findByStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_REHABILITATION"));

        verify(service).findByStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/rescue-cases").param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status").exists());
    }

    @Test
    void shouldChangeRescueCaseStatus() throws Exception {
        RescueCaseResponse response = rescueCase(RescueStatus.READY_FOR_RELEASE);

        when(service.changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READY_FOR_RELEASE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_RELEASE"));

        verify(service).changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class));
    }

    @Test
    void shouldReturn400WhenChangeStatusRequestIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.status").value("Status is required"));

        verify(service, never()).changeStatus(anyString(), any());
    }

    @Test
    void shouldReturn409WhenStatusTransitionIsInvalid() throws Exception {
        when(service.changeStatus(eq("RES-001"), any()))
                .thenThrow(new BusinessRuleException("Invalid status transition"));

        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READY_FOR_RELEASE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Invalid status transition"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn400WhenStatusEnumInJsonIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "FLYING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").value("Check JSON syntax and enum values"));

        verify(service, never()).changeStatus(anyString(), any());
    }

    @Test
    void shouldReturn500WhenUnexpectedExceptionOccurs() throws Exception {
        when(service.findByCode("RES-500"))
                .thenThrow(new IllegalStateException("Sensitive database details"));

        mockMvc.perform(get("/api/rescue-cases/{caseCode}", "RES-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).findByCode("RES-500");
    }

    private RescueCaseResponse rescueCase(RescueStatus rescueStatus) {
        return new RescueCaseResponse(
                1L,
                "RES-2026-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                rescueStatus,
                "DB-CAR",
                "AN-2026-001"
        );
    }
}
