package com.example.marketplace.service;

import com.example.marketplace.domain.UserAccount;
import com.example.marketplace.dto.UserDtos.CreateUserRequest;
import com.example.marketplace.dto.UserDtos.UserResponse;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserAccountRepository userRepository;

    public UserService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse create(CreateUserRequest request) {
        UserAccount saved = userRepository.save(new UserAccount(request.fullName(), request.email()));
        return toResponse(saved);
    }

    UserAccount requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " was not found"));
    }

    static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
