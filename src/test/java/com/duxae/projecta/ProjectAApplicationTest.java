package com.duxae.projecta;

import com.duxae.projecta.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

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
}
