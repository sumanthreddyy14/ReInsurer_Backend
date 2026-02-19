package com.cts.backend;

import com.cts.backend.treaty.dto.ReinsurerUiDTO;
import com.cts.backend.treaty.repositories.ReinsurerRepository;
import com.cts.backend.treaty.service.ReinsurerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Ensures MySQL stays clean by rolling back after each test
class ReinsurerServiceIntegrationTest {

    @Autowired
    private ReinsurerService reinsurerService;

    @Autowired
    private ReinsurerRepository repo;

    @Test
    @DisplayName("Should save a reinsurer and generate a custom ID (e.g., R001)")
    void create_AutoGenerateId_Success() {
        // Arrange
        ReinsurerUiDTO dto = new ReinsurerUiDTO();
        dto.setName("Swiss Re");
        dto.setContactInfo("contact@swiss.re");

        // Act
        ReinsurerUiDTO saved = reinsurerService.create(dto);

        // Assert
        assertNotNull(saved.getReinsurerId());
        assertTrue(saved.getReinsurerId().startsWith("R"));
        assertEquals("Swiss Re", saved.getName());

        // Verify it exists in MySQL
        assertTrue(repo.findByReinsurerId(saved.getReinsurerId()).isPresent());
    }

    @Test
    @DisplayName("Should save a reinsurer with a manually provided ID")
    void create_ManualId_Success() {
        // Arrange
        ReinsurerUiDTO dto = new ReinsurerUiDTO();
        dto.setReinsurerId("M999");
        dto.setName("Manual Corp");

        // Act
        ReinsurerUiDTO saved = reinsurerService.create(dto);

        // Assert
        assertEquals("M999", saved.getReinsurerId());
        assertTrue(repo.existsByReinsurerId("M999"));
    }

    @Test
    @DisplayName("Should throw CONFLICT when trying to use an existing ReinsurerId")
    void create_DuplicateId_ThrowsConflict() {
        // Arrange: Save one first
        ReinsurerUiDTO dto1 = new ReinsurerUiDTO();
        dto1.setReinsurerId("DUP01");
        dto1.setName("Original");
        reinsurerService.create(dto1);

        // Act & Assert
        ReinsurerUiDTO dto2 = new ReinsurerUiDTO();
        dto2.setReinsurerId("DUP01");
        dto2.setName("Copycat");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reinsurerService.create(dto2));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already exists"));
    }

    @Test
    @DisplayName("Should throw BAD_REQUEST when name is missing")
    void create_NoName_ThrowsBadRequest() {
        ReinsurerUiDTO dto = new ReinsurerUiDTO();
        dto.setName(""); // Blank name

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reinsurerService.create(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Should list all reinsurers stored in MySQL")
    void list_ReturnsAllRecords() {
        // Arrange
        ReinsurerUiDTO r1 = new ReinsurerUiDTO();
        r1.setName("Company A");
        reinsurerService.create(r1);

        // Act
        List<ReinsurerUiDTO> list = reinsurerService.list();

        // Assert
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(r -> r.getName().equals("Company A")));
    }

    @Test
    @DisplayName("Should delete a reinsurer and confirm it's gone from DB")
    void delete_RemovesFromDb() {
        // Arrange
        ReinsurerUiDTO dto = new ReinsurerUiDTO();
        dto.setName("To Be Deleted");
        ReinsurerUiDTO saved = reinsurerService.create(dto);
        String id = saved.getReinsurerId();

        // Act
        reinsurerService.delete(id);

        // Assert
        assertFalse(repo.findByReinsurerId(id).isPresent());
    }
}