package com.iba.controller;

import com.iba.dto.NotaDTO;
import com.iba.service.NotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notas")
@RequiredArgsConstructor
public class NotaController {

    private final NotaService service;

    @GetMapping
    public ResponseEntity<List<NotaDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<List<NotaDTO>> findByAluno(@PathVariable Long alunoId) {
        return ResponseEntity.ok(service.findByAlunoId(alunoId));
    }

    @GetMapping("/materia/{materiaId}")
    public ResponseEntity<List<NotaDTO>> findByMateria(@PathVariable Long materiaId) {
        return ResponseEntity.ok(service.findByMateriaId(materiaId));
    }

    @PostMapping
    public ResponseEntity<NotaDTO> save(@Valid @RequestBody NotaDTO notaDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(notaDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/matricula/{matriculaId}")
public ResponseEntity<List<NotaDTO>> findByMatricula(@PathVariable Long matriculaId) {
    return ResponseEntity.ok(service.findByMatriculaId(matriculaId));
}

}