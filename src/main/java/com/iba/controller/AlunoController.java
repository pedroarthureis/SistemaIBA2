package com.iba.controller;

import com.iba.dto.AlunoDTO;
import com.iba.service.AlunoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/alunos")
@RequiredArgsConstructor
public class AlunoController {

    private final AlunoService service;

    @GetMapping
    public ResponseEntity<List<AlunoDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlunoDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    // REMOVER este endpoint - aluno não tem mais curso diretamente
    // @GetMapping("/curso/{cursoId}")
    // public ResponseEntity<List<AlunoDTO>> findByCurso(@PathVariable Long cursoId) {
    //     return ResponseEntity.ok(service.findByCursoId(cursoId));
    // }

    @GetMapping("/search")
    public ResponseEntity<List<AlunoDTO>> search(@RequestParam String nome) {
        return ResponseEntity.ok(service.searchByNome(nome));
    }

    @PostMapping
    public ResponseEntity<AlunoDTO> create(@Valid @RequestBody AlunoDTO alunoDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(alunoDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoDTO> update(@PathVariable Long id, @Valid @RequestBody AlunoDTO alunoDTO) {
        return ResponseEntity.ok(service.update(id, alunoDTO));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AlunoDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}