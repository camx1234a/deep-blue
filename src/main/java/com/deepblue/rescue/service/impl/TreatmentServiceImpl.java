package com.deepblue.rescue.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(
	    AnimalRepository animalRepository,
	    SpecialistRepository specialistRepository,
	    TreatmentRepository treatmentRepository,
	    TreatmentMapper mapper
    ) {
	this.animalRepository = animalRepository;
	this.specialistRepository = specialistRepository;
	this.treatmentRepository = treatmentRepository;
	this.mapper = mapper;
    }

    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
	return treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
		.stream()
		.map(mapper::toResponse)
		.toList();
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {
	Animal animal = animalRepository.findByAnimalCode(request.animalCode())
		.orElseThrow(() -> new ResourceNotFoundException(
			"Animal not found: " + request.animalCode()
		));

	Specialist specialist = specialistRepository
		.findByProfessionalCode(request.specialistCode())
		.orElseThrow(() -> new ResourceNotFoundException(
			"Specialist not found: " + request.specialistCode()
		));

	if (!specialist.isActive()) {
	    throw new BusinessRuleException(
		    "Cannot register treatment with an inactive specialist: "
			    + request.specialistCode()
	    );
	}

	if (animal.getRescueCase() == null) {
	    throw new BusinessRuleException(
		    "Animal has no associated rescue case: " + request.animalCode()
	    );
	}

	RescueStatus status = animal.getRescueCase().getStatus();
	if (status == RescueStatus.RELEASED || status == RescueStatus.CLOSED) {
	    throw new BusinessRuleException(
		    "Cannot register treatment for a released or closed case"
	    );
	}

	LocalDateTime rescueDate = animal.getRescueCase().getRescueDate().atStartOfDay();
	if (request.performedAt().isBefore(rescueDate)) {
	    throw new BusinessRuleException(
		    "Treatment date cannot be before the rescue date"
	    );
	}

	Treatment treatment = new Treatment();
	treatment.setAnimal(animal);
	treatment.setSpecialist(specialist);
	treatment.setPerformedAt(request.performedAt());
	treatment.setType(request.type());
	treatment.setDescription(request.description());

	return mapper.toResponse(treatmentRepository.save(treatment));
    }
}
