package com.duxae.projecta.service;

import com.duxae.projecta.domain.Note;
import com.duxae.projecta.repository.NoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Бизнес-логика над Note. Работает только с доменом и репозиторием:
 * не знает ни про HTTP, ни про DTO.
 */
@Service
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Page<Note> findAll(Pageable pageable) {
        return noteRepository.findAll(pageable);
    }

    public Note findById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note", id));
    }

    @Transactional
    public Note create(String title, String content) {
        Note note = new Note(title, content);
        return noteRepository.save(note);
    }

    @Transactional
    public Note update(Long id, String title, String content) {
        Note note = findById(id);
        note.setTitle(title);
        note.setContent(content);
        return noteRepository.save(note);
    }

    @Transactional
    public void delete(Long id) {
        if (!noteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Note", id);
        }
        noteRepository.deleteById(id);
    }
}
