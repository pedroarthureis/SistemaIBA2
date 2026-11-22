package com.iba.controller;

import com.iba.dto.DoacaoDTO;
import com.iba.dto.DashboardDoacoesDTO;
import com.iba.service.DoacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doacoes")
@RequiredArgsConstructor
public class DoacaoController {

    private final DoacaoService service;

    @GetMapping
    public ResponseEntity<List<DoacaoDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoacaoDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DoacaoDTO>> findByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.findByStatus(status));
    }

    @GetMapping("/periodo")
    public ResponseEntity<List<DoacaoDTO>> findByPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(service.findByPeriodo(inicio, fim));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDoacoesDTO> getDashboard() {
        return ResponseEntity.ok(service.getDashboardData());
    }

    @PostMapping
    public ResponseEntity<DoacaoDTO> create(@Valid @RequestBody DoacaoDTO doacaoDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(doacaoDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoacaoDTO> update(@PathVariable Long id, @Valid @RequestBody DoacaoDTO doacaoDTO) {
        return ResponseEntity.ok(service.update(id, doacaoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}