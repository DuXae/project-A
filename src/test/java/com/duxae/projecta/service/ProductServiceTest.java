package com.duxae.projecta.service;

import com.duxae.projecta.domain.Product;
import com.duxae.projecta.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void saveAllDelegatesAndReturnsSavedEntities() {
        List<Product> input = List.of(
                new Product("Гвоздь 50мм", 100),
                new Product("Шуруп 3x20", 250)
        );
        when(productRepository.saveAll(input)).thenReturn(input);

        List<Product> result = productService.saveAll(input);

        assertThat(result).hasSize(2);
        verify(productRepository).saveAll(input);
    }

    @Test
    void saveAllWithEmptyListDoesNotFail() {
        when(productRepository.saveAll(any())).thenReturn(List.of());

        assertThat(productService.saveAll(List.of())).isEmpty();
    }

    @Test
    void findAllDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(new Product("Товар", 1))));

        Page<Product> page = productService.findAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(productRepository).findAll(pageable);
    }
}
