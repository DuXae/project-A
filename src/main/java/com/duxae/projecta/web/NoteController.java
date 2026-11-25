package com.duxae.projecta.web;

import com.duxae.projecta.domain.Note;
import com.duxae.projecta.service.NoteService;
import com.duxae.projecta.web.dto.NoteRequest;
import com.duxae.projecta.web.dto.NoteResponse;
import com.duxae.projecta.web.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Controller: HTTP-граница. Только разбор запроса, вызов сервиса
 * и формирование ответа. Никакой бизнес-логики и работы с JPA.
 */
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public PageResponse<NoteResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return PageResponse.from(noteService.findAll(pageable), NoteResponse::from);
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable Long id) {
        return NoteResponse.from(noteService.findById(id));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody NoteRequest request,
                                               UriComponentsBuilder uriBuilder) {
        Note created = noteService.create(request.title(), request.content());
        URI location = uriBuilder.path("/api/v1/notes/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(NoteResponse.from(created));
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable Long id,
                              @Valid @RequestBody NoteRequest request) {
        return NoteResponse.from(noteService.update(id, request.title(), request.content()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        noteService.delete(id);
    }
}
