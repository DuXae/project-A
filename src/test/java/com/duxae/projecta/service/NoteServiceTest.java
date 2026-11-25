package com.duxae.projecta.service;

import com.duxae.projecta.domain.Note;
import com.duxae.projecta.repository.NoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private NoteService noteService;

    @Test
    void findAllDelegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Note> page = new PageImpl<>(List.of(new Note("t", "c")));
        when(noteRepository.findAll(pageable)).thenReturn(page);

        Page<Note> result = noteService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(noteRepository).findAll(pageable);
    }

    @Test
    void findByIdReturnsEntity() {
        Note note = new Note("title", "content");
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        assertThat(noteService.findById(1L)).isSameAs(note);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(noteRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void createSavesNewEntity() {
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        Note created = noteService.create("hello", "world");

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("hello");
        assertThat(captor.getValue().getContent()).isEqualTo("world");
        assertThat(created.getTitle()).isEqualTo("hello");
    }

    @Test
    void updateMutatesExistingEntity() {
        Note note = new Note("old", "old content");
        when(noteRepository.findById(7L)).thenReturn(Optional.of(note));
        when(noteRepository.save(note)).thenReturn(note);

        Note updated = noteService.update(7L, "new", "new content");

        assertThat(updated.getTitle()).isEqualTo("new");
        assertThat(updated.getContent()).isEqualTo("new content");
    }

    @Test
    void updateThrowsWhenMissing() {
        when(noteRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.update(7L, "t", "c"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(noteRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingEntity() {
        when(noteRepository.existsById(3L)).thenReturn(true);

        noteService.delete(3L);

        verify(noteRepository).deleteById(3L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(noteRepository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> noteService.delete(3L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(noteRepository, never()).deleteById(any());
    }
}
