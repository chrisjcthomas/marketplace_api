package com.example.marketplace.repository;

import com.example.marketplace.domain.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, Long> {
}
