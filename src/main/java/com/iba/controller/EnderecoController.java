package com.iba.controller;

import com.iba.service.ViaCepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/endereco")
@RequiredArgsConstructor
public class EnderecoController {

    private final ViaCepService viaCepService;

    @GetMapping("/cep/{cep}")
    public ResponseEntity<Map<String, String>> buscarCep(@PathVariable String cep) {
        Map<String, String> endereco = viaCepService.buscarEnderecoPorCep(cep);
        return ResponseEntity.ok(endereco);
    }
}