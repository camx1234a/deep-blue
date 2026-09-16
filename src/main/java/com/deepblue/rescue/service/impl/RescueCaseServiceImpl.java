package com.deepblue.rescue.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.RescueCaseService;

@Service
@Transactional(readOnly = true)
public class RescueCaseServiceImpl implements RescueCaseService {

    private final RescueCaseRepository repository;
    private final RescueCaseMapper mapper;

    public RescueCaseServiceImpl(
            RescueCaseRepository repository,
            RescueCaseMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RescueCaseResponse findByCode(String caseCode) {
        return repository.findByCaseCode(caseCode)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Rescue case not found: " + caseCode
            ));
    }

    @Override
    public List<RescueCaseResponse> findByStatus(RescueStatus status) {
        throw new UnsupportedOperationException("Unimplemented method 'findByStatus'");
    }

    @Override
    public RescueCaseResponse changeStatus(String caseCode, ChangeRescueStatusRequest request) {
        throw new UnsupportedOperationException("Unimplemented method 'changeStatus'");
    }
}