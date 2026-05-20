package com.example.marketplace.service;

import com.example.marketplace.domain.Business;
import com.example.marketplace.domain.Product;
import com.example.marketplace.dto.ProductDtos.CreateProductRequest;
import com.example.marketplace.dto.ProductDtos.ProductResponse;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final BusinessService businessService;

    public ProductService(ProductRepository productRepository, BusinessService businessService) {
        this.productRepository = productRepository;
        this.businessService = businessService;
    }

    public ProductResponse create(CreateProductRequest request) {
        Business business = businessService.requireBusiness(request.businessId());
        Product saved = productRepository.save(new Product(
                request.name(),
                request.price(),
                request.stockQuantity(),
                business
        ));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductService::toResponse)
                .toList();
    }

    Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getBusiness().getId(),
                product.getBusiness().getName()
        );
    }
}
