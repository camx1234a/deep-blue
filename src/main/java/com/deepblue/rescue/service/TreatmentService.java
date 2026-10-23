package com.deepblue.rescue.service;

import java.util.List;

import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;

public interface TreatmentService {

	TreatmentResponse register(CreateTreatmentRequest request);

	List<TreatmentResponse> findByAnimalCode(String animalCode);
}
