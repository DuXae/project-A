package com.duxae.projecta;

import com.duxae.projecta.domain.Product;
import com.duxae.projecta.repository.ProductRepository;
import com.duxae.projecta.service.NoteService;
import com.duxae.projecta.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что контекст поднимается, Flyway применяет миграции,
 * а сервис реально ходит в БД (AC-5).
 */
@SpringBootTest
@ActiveProfiles("test")
class ProjectAApplicationTest {

    @Autowired
    private NoteService noteService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void contextLoadsAndCrudWorksAgainstRealDatabase() {
        var created = noteService.create("integration", "works");

        assertThat(created.getId()).isNotNull();
        assertThat(noteService.findById(created.getId()).getTitle()).isEqualTo("integration");

        created.setTitle("updated");
        noteService.update(created.getId(), "updated", "works");

        assertThat(noteService.findAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);

        noteService.delete(created.getId());
        assertThat(noteService.findAll(PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    /**
     * AC-4, AC-5: мягкое удаление скрывает запись из списка,
     * но строка остаётся в БД.
     */
    @Test
    void softDeletedProductIsHiddenButStillInDatabase() {
        Product saved = productService.saveAll(List.of(new Product("Мягко удаляемый", 5))).get(0);
        Long id = saved.getId();

        assertThat(productService.findAll(PageRequest.of(0, 100)).getContent())
                .extracting(Product::getId)
                .contains(id);

        productService.softDelete(id);

        assertThat(productService.findAll(PageRequest.of(0, 100)).getContent())
                .extracting(Product::getId)
                .doesNotContain(id);

        // запись физически на месте и помечена датой
        Product inDb = productRepository.findById(id).orElseThrow();
        assertThat(inDb.getDeletedAt()).isNotNull();
        assertThat(inDb.isDeleted()).isTrue();
    }

    @Test
    void bulkSoftDeleteHidesAllAndKeepsRows() {
        List<Product> saved = productService.saveAll(List.of(
                new Product("Пачка A", 1),
                new Product("Пачка B", 2)
        ));
        List<Long> ids = saved.stream().map(Product::getId).toList();

        int deleted = productService.softDeleteAll(ids);

        assertThat(deleted).isEqualTo(2);
        assertThat(productService.findAll(PageRequest.of(0, 100)).getContent())
                .extracting(Product::getId)
                .doesNotContainAnyElementsOf(ids);
        assertThat(productRepository.findAllById(ids))
                .hasSize(2)
                .allMatch(Product::isDeleted);
    }
}
