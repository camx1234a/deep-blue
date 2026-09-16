package com.deepblue.rescue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldFindAnimalByCode() {
        Animal animal = createAnimal(RescueStatus.IN_REHABILITATION);
        AnimalResponse response = new AnimalResponse(
                1L, "AN-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION
        );

        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findByCode("AN-001")).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
        when(repository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindAnimalsInRehabilitation() {
        Animal animal = createAnimal(RescueStatus.IN_REHABILITATION);
        AnimalResponse response = new AnimalResponse(
                1L, "AN-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION
        );

        when(repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findAnimalsInRehabilitation()).containsExactly(response);
    }

    @Test
    void shouldAllowTreatmentForEvaluationOrRehabilitation() {
        Animal animal = createAnimal(RescueStatus.UNDER_EVALUATION);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-001")).isTrue();
    }

    @Test
    void shouldNotAllowTreatmentForReleasedAnimal() {
        Animal animal = createAnimal(RescueStatus.RELEASED);
        when(repository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));

        assertThat(service.canReceiveTreatment("AN-001")).isFalse();
    }

    private Animal createAnimal(RescueStatus status) {
        RescueCase rescueCase = new RescueCase(
                "RES-001", LocalDate.of(2026, 8, 20), "Playa", status
        );
        Animal animal = new Animal(
                "AN-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);
        return animal;
    }
}
