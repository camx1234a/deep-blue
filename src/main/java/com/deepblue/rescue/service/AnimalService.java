package com.deepblue.rescue.service;

import java.util.List;

import com.deepblue.rescue.dto.response.AnimalResponse;

public interface AnimalService {

    AnimalResponse findByCode(String animalCode);

    List<AnimalResponse> findAnimalsInRehabilitation();

    boolean canReceiveTreatment(String animalCode);
}
