package com.example.marketplace.controller;

import com.example.marketplace.dto.BusinessDtos.BusinessResponse;
import com.example.marketplace.dto.BusinessDtos.CreateBusinessRequest;
import com.example.marketplace.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessResponse create(@Valid @RequestBody CreateBusinessRequest request) {
        return businessService.create(request);
    }
}
