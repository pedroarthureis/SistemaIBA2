package com.iba.controller;

import com.iba.dto.MateriaDTO;
import com.iba.service.MateriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService service;

    @GetMapping
    public ResponseEntity<List<MateriaDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MateriaDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<MateriaDTO> create(@Valid @RequestBody MateriaDTO materiaDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(materiaDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MateriaDTO> update(@PathVariable Long id, @Valid @RequestBody MateriaDTO materiaDTO) {
        return ResponseEntity.ok(service.update(id, materiaDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}