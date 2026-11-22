package com.iba.controller;

import com.iba.dto.MatriculaDTO;
import com.iba.service.MatriculaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService service;

    @GetMapping
    public ResponseEntity<List<MatriculaDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatriculaDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<List<MatriculaDTO>> findByAluno(@PathVariable Long alunoId) {
        return ResponseEntity.ok(service.findByAlunoId(alunoId));
    }

    @GetMapping("/curso/{cursoId}/ano/{ano}/semestre/{semestre}")
    public ResponseEntity<List<MatriculaDTO>> findByCursoAndPeriodo(
            @PathVariable Long cursoId, 
            @PathVariable Integer ano, 
            @PathVariable Integer semestre) {
        return ResponseEntity.ok(service.findByCursoAndPeriodo(cursoId, ano, semestre));
    }

    @GetMapping("/curso/{cursoId}/ativas")
    public ResponseEntity<List<MatriculaDTO>> findAtivasByCurso(@PathVariable Long cursoId) {
        return ResponseEntity.ok(service.findAtivasByCurso(cursoId));
    }

    @PostMapping
    public ResponseEntity<MatriculaDTO> create(@Valid @RequestBody MatriculaDTO matriculaDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(matriculaDTO));
    }

    @PutMapping("/{id}/materias")
    public ResponseEntity<MatriculaDTO> updateMaterias(@PathVariable Long id, @RequestBody List<Long> materiasIds) {
        return ResponseEntity.ok(service.updateMaterias(id, materiasIds));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<MatriculaDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}