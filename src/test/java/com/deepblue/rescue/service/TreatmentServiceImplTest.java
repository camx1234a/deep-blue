package com.deepblue.rescue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    @Test
    void shouldRegisterTreatment() {
	Animal animal = createAnimal(RescueStatus.IN_REHABILITATION);
	Specialist specialist = new Specialist(
		"SPEC-001", "Elena", "Vargas", "elena@test.com", true
	);
	CreateTreatmentRequest request = new CreateTreatmentRequest(
		"AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
		TreatmentType.WOUND_CARE, "Flipper care"
	);
	Treatment treatment = new Treatment();
	TreatmentResponse response = new TreatmentResponse(
		1L, "AN-001", "SPEC-001", request.performedAt(),
		request.type(), request.description()
	);

	when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
	when(specialistRepository.findByProfessionalCode("SPEC-001"))
		.thenReturn(Optional.of(specialist));
	when(treatmentRepository.save(any(Treatment.class))).thenReturn(treatment);
	when(mapper.toResponse(treatment)).thenReturn(response);

	TreatmentResponse result = service.register(request);

	assertThat(result).isEqualTo(response);
	verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldRejectInactiveSpecialist() {
	Animal animal = createAnimal(RescueStatus.IN_REHABILITATION);
	Specialist specialist = new Specialist(
		"SPEC-001", "Elena", "Vargas", "elena@test.com", false
	);
	CreateTreatmentRequest request = createRequest();

	when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
	when(specialistRepository.findByProfessionalCode("SPEC-001"))
		.thenReturn(Optional.of(specialist));

	assertThatThrownBy(() -> service.register(request))
		.isInstanceOf(BusinessRuleException.class);

	verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentForReleasedCase() {
	Animal animal = createAnimal(RescueStatus.RELEASED);
	Specialist specialist = new Specialist(
		"SPEC-001", "Elena", "Vargas", "elena@test.com", true
	);

	when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
	when(specialistRepository.findByProfessionalCode("SPEC-001"))
		.thenReturn(Optional.of(specialist));

	assertThatThrownBy(() -> service.register(createRequest()))
		.isInstanceOf(BusinessRuleException.class);

	verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAnimalDoesNotExist() {
	when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.empty());

	assertThatThrownBy(() -> service.register(createRequest()))
		.isInstanceOf(ResourceNotFoundException.class);

	verify(specialistRepository, never()).findByProfessionalCode(any());
	verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenSpecialistDoesNotExist() {
	when(animalRepository.findByAnimalCode("AN-001"))
		.thenReturn(Optional.of(createAnimal(RescueStatus.IN_REHABILITATION)));
	when(specialistRepository.findByProfessionalCode("SPEC-001"))
		.thenReturn(Optional.empty());

	assertThatThrownBy(() -> service.register(createRequest()))
		.isInstanceOf(ResourceNotFoundException.class);

	verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
	Animal animal = createAnimal(RescueStatus.IN_REHABILITATION);
	Specialist specialist = new Specialist(
		"SPEC-001", "Elena", "Vargas", "elena@test.com", true
	);
	CreateTreatmentRequest request = new CreateTreatmentRequest(
		"AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 19, 9, 0),
		TreatmentType.OBSERVATION, "Too early"
	);

	when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
	when(specialistRepository.findByProfessionalCode("SPEC-001"))
		.thenReturn(Optional.of(specialist));

	assertThatThrownBy(() -> service.register(request))
		.isInstanceOf(BusinessRuleException.class);

	verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldFindTreatmentsByAnimalCode() {
	Treatment firstTreatment = new Treatment();
	Treatment secondTreatment = new Treatment();
	TreatmentResponse firstResponse = new TreatmentResponse(
		1L, "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 20, 9, 0),
		TreatmentType.WOUND_CARE, "First"
	);
	TreatmentResponse secondResponse = new TreatmentResponse(
		2L, "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
		TreatmentType.OBSERVATION, "Second"
	);

	when(treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-001"))
		.thenReturn(List.of(firstTreatment, secondTreatment));
	when(mapper.toResponse(firstTreatment)).thenReturn(firstResponse);
	when(mapper.toResponse(secondTreatment)).thenReturn(secondResponse);

	List<TreatmentResponse> result = service.findByAnimalCode("AN-001");

	assertThat(result).containsExactly(firstResponse, secondResponse);
	verify(treatmentRepository).findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-001");
    }

    private CreateTreatmentRequest createRequest() {
	return new CreateTreatmentRequest(
		"AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
		TreatmentType.OBSERVATION, "General checkup"
	);
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
