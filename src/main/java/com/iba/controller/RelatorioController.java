package com.iba.controller;

import com.iba.dto.RelatorioDTO;
import com.iba.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService service;

    @GetMapping("/boletim/{alunoId}")
    public ResponseEntity<RelatorioDTO> gerarBoletim(@PathVariable Long alunoId) {
        return ResponseEntity.ok(service.gerarBoletim(alunoId));
    }

    @GetMapping("/rendimento-cursos")
    public ResponseEntity<RelatorioDTO> gerarRendimentoPorCurso() {
        return ResponseEntity.ok(service.gerarRendimentoPorCurso());
    }

    @GetMapping("/taxa-aprovacao")
    public ResponseEntity<RelatorioDTO> gerarTaxaAprovacao() {
        return ResponseEntity.ok(service.gerarTaxaAprovacao());
    }
}