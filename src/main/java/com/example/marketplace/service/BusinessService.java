package com.example.marketplace.service;

import com.example.marketplace.domain.Business;
import com.example.marketplace.domain.UserAccount;
import com.example.marketplace.dto.BusinessDtos.BusinessResponse;
import com.example.marketplace.dto.BusinessDtos.CreateBusinessRequest;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.BusinessRepository;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final UserService userService;

    public BusinessService(BusinessRepository businessRepository, UserService userService) {
        this.businessRepository = businessRepository;
        this.userService = userService;
    }

    public BusinessResponse create(CreateBusinessRequest request) {
        UserAccount owner = userService.requireUser(request.ownerUserId());
        Business saved = businessRepository.save(new Business(request.name(), request.category(), owner));
        return toResponse(saved);
    }

    Business requireBusiness(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business " + id + " was not found"));
    }

    static BusinessResponse toResponse(Business business) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getCategory(),
                business.getOwner().getId()
        );
    }
}
