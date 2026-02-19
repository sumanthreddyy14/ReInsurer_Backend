package com.cts.backend;
import com.cts.backend.treaty.dto.TreatyUiDTO;
import com.cts.backend.treaty.entity.Reinsurer;
import com.cts.backend.treaty.entity.Treaty;
import com.cts.backend.treaty.repositories.ReinsurerRepository;
import com.cts.backend.treaty.repositories.TreatyRepository;
import com.cts.backend.treaty.service.TreatyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatyServiceTest {

    @Mock
    private TreatyRepository repo;

    @Mock
    private ReinsurerRepository reinsurerRepo;

    @InjectMocks
    private TreatyService treatyService;

    private Reinsurer mockReinsurer;
    private Treaty mockTreaty;
    private TreatyUiDTO inputDto;

    @BeforeEach
    void setUp() {
        mockReinsurer = new Reinsurer();
        mockReinsurer.setReinsurerId("R101");
        mockReinsurer.setName("Global Re");

        mockTreaty = new Treaty();
        mockTreaty.setId(1L);
        mockTreaty.setTreatyId("T001");
        mockTreaty.setReinsurer(mockReinsurer);
        mockTreaty.setStartDate(LocalDate.now());
        mockTreaty.setEndDate(LocalDate.now().plusYears(1));
        mockTreaty.setStatus(Treaty.TreatyStatus.ACTIVE);
        mockTreaty.setTreatyType(Treaty.TreatyType.PROPORTIONAL);

        inputDto = new TreatyUiDTO();
        inputDto.setReinsurerId("R101");
        inputDto.setStartDate("2023-01-01");
        inputDto.setEndDate("2024-01-01");
        inputDto.setStatus("ACTIVE");
        inputDto.setTreatyType("PROPORTIONAL");
    }

    @Test
    @DisplayName("Should return a treaty when valid ID is provided")
    void get_Success() {
        // Arrange
        when(repo.findByTreatyId("T001")).thenReturn(Optional.of(mockTreaty));

        // Act
        TreatyUiDTO result = treatyService.get("T001");

        // Assert
        assertNotNull(result);
        assertEquals("T001", result.getTreatyId());
        assertEquals("Global Re", result.getReinsurerName());
    }

    @Test
    @DisplayName("Should throw 404 when treaty is not found")
    void get_NotFound() {
        when(repo.findByTreatyId("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> treatyService.get("NON_EXISTENT"));
    }

    @Test
    @DisplayName("Should generate a new TreatyId (T002) if not provided in DTO")
    void create_GenerateId() {
        // Arrange
        when(reinsurerRepo.findByReinsurerId("R101")).thenReturn(Optional.of(mockReinsurer));
        // Mocking the ID generation logic (finding max ID)
        when(repo.findAll()).thenReturn(java.util.List.of(mockTreaty));
        when(repo.existsByTreatyId("T002")).thenReturn(false);
        when(repo.save(any(Treaty.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        TreatyUiDTO result = treatyService.create(inputDto);

        // Assert
        assertEquals("T002", result.getTreatyId());
        verify(repo).save(any(Treaty.class));
    }

    @Test
    @DisplayName("Should throw Conflict if manual TreatyId already exists")
    void create_Conflict() {
        // Arrange
        inputDto.setTreatyId("T001");
        when(reinsurerRepo.findByReinsurerId("R101")).thenReturn(Optional.of(mockReinsurer));
        when(repo.existsByTreatyId("T001")).thenReturn(true);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> treatyService.create(inputDto));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Should update fields correctly")
    void update_Success() {
        // Arrange
        TreatyUiDTO updateDto = new TreatyUiDTO();
        updateDto.setStatus("EXPIRED");

        when(repo.findByTreatyId("T001")).thenReturn(Optional.of(mockTreaty));
        when(repo.save(any(Treaty.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        TreatyUiDTO result = treatyService.update("T001", updateDto);

        // Assert
        assertEquals("EXPIRED", result.getStatus());
        verify(repo).save(any(Treaty.class));
    }

    @Test
    @DisplayName("Should delete treaty if it exists")
    void delete_Success() {
        // Arrange
        when(repo.findByTreatyId("T001")).thenReturn(Optional.of(mockTreaty));

        // Act
        treatyService.delete("T001");

        // Assert
        verify(repo, times(1)).delete(mockTreaty);
    }
}