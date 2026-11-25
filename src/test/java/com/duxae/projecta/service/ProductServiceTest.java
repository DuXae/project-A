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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
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

    /**
     * FR-7: список строится только по активным записям.
     */
    @Test
    void findAllUsesActiveOnlyQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.findAllByDeletedAtIsNull(pageable))
                .thenReturn(new PageImpl<>(List.of(new Product("Товар", 1))));

        Page<Product> page = productService.findAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(productRepository).findAllByDeletedAtIsNull(pageable);
        verify(productRepository, never()).findAll(pageable);
    }

    @Test
    void softDeleteMarksEntityAndSaves() {
        Product product = new Product("Молоток", 3);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        productService.softDelete(1L);

        assertThat(product.isDeleted()).isTrue();
        assertThat(product.getDeletedAt()).isNotNull();
        verify(productRepository).save(product);
    }

    @Test
    void softDeleteThrowsForMissingOrAlreadyDeleted() {
        when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.softDelete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(productRepository, never()).save(any());
    }

    @Test
    void softDeleteAllMarksOnlyFoundEntities() {
        Product a = new Product("A", 1);
        Product b = new Product("B", 2);
        when(productRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(a, b));
        when(productRepository.saveAll(List.of(a, b))).thenReturn(List.of(a, b));

        int deleted = productService.softDeleteAll(List.of(1L, 2L, 3L));

        assertThat(deleted).isEqualTo(2);
        assertThat(a.isDeleted()).isTrue();
        assertThat(b.isDeleted()).isTrue();
    }

    @Test
    void softDeleteAllWithNoMatchesReturnsZero() {
        when(productRepository.findAllByIdInAndDeletedAtIsNull(anyCollection())).thenReturn(List.of());

        assertThat(productService.softDeleteAll(List.of(7L, 8L))).isZero();
        verify(productRepository).saveAll(List.of());
    }
}
