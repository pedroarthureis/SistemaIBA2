package com.iba.service;

import com.iba.dto.RelatorioDTO;
import com.iba.model.AlunoModel;
import com.iba.model.CursoModel;
import com.iba.model.MatriculaModel;
import com.iba.repository.AlunoRepository;
import com.iba.repository.CursoRepository;
import com.iba.repository.MatriculaRepository;
import com.iba.repository.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final AlunoRepository alunoRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;
    private final NotaRepository notaRepository;

    @Transactional(readOnly = true)
    public RelatorioDTO gerarBoletim(Long alunoId) {
        AlunoModel aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
        
        List<MatriculaModel> matriculas = matriculaRepository.findByAlunoIdWithNotas(alunoId);
        
        RelatorioDTO relatorio = new RelatorioDTO();
        relatorio.setTipo("BOLETIM");
        
        String nomeCurso = "Não matriculado";
        for (MatriculaModel m : matriculas) {
            if (m.getStatus() == MatriculaModel.StatusMatricula.CURSANDO || nomeCurso.equals("Não matriculado")) {
                nomeCurso = m.getCurso().getNome();
                if (m.getStatus() == MatriculaModel.StatusMatricula.CURSANDO) break;
            }
        }
        
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("aluno", aluno.getNome());
        dados.put("matricula", aluno.getMatricula());
        dados.put("curso", nomeCurso);
        dados.put("mediaGeral", calcularMediaGeralAluno(alunoId));
        
        List<Map<String, Object>> linhas = new ArrayList<>();
        for (MatriculaModel matricula : matriculas) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("ano", matricula.getAno());
            linha.put("semestre", matricula.getSemestre());
            linha.put("status", matricula.getStatus());
            linha.put("curso", matricula.getCurso().getNome());
            linha.put("media", (matricula.getStatus() == MatriculaModel.StatusMatricula.APROVADO ||
                    matricula.getStatus() == MatriculaModel.StatusMatricula.REPROVADO)
                    ? matricula.getMediaFinal() : null);
            
            // Detalhamento por matéria
            List<Map<String, Object>> materiasNotas = new ArrayList<>();
            // Incluir todas as matérias do curso, mesmo sem avaliação registrada.
            Map<Long, Map<String, Object>> materiasMap = new LinkedHashMap<>();
            for (var materia : matricula.getMateriasEfetivas()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("materia", materia.getNome());
                item.put("materiaId", materia.getId());
                item.put("ciclo1", null);
                item.put("ciclo2", null);
                item.put("ciclo3", null);
                materiasMap.put(materia.getId(), item);
            }

            for (var nota : matricula.getNotas()) {
                Long materiaId = nota.getMateria().getId();
                if (!materiasMap.containsKey(materiaId)) {
                    Map<String, Object> materiaNota = new LinkedHashMap<>();
                    materiaNota.put("materia", nota.getMateria().getNome());
                    materiaNota.put("materiaId", materiaId);
                    materiaNota.put("ciclo1", null);
                    materiaNota.put("ciclo2", null);
                    materiaNota.put("ciclo3", null);
                    materiasMap.put(materiaId, materiaNota);
                }
                
                Map<String, Object> materiaNota = materiasMap.get(materiaId);
                if (nota.getCiclo() == 1) materiaNota.put("ciclo1", nota.getValor());
                if (nota.getCiclo() == 2) materiaNota.put("ciclo2", nota.getValor());
                if (nota.getCiclo() == 3) materiaNota.put("ciclo3", nota.getValor());
            }
            
            // Calcular médias
            for (Map<String, Object> materiaNota : materiasMap.values()) {
                Double ciclo1 = (Double) materiaNota.get("ciclo1");
                Double ciclo2 = (Double) materiaNota.get("ciclo2");
                Double ciclo3 = (Double) materiaNota.get("ciclo3");
                boolean completo = ciclo1 != null && ciclo2 != null && ciclo3 != null;
                Double media = completo ? (ciclo1 + ciclo2 + ciclo3) / 3 : null;
                materiaNota.put("media", media);
                materiaNota.put("situacao", completo ? (media >= 60 ? "APROVADO" : "REPROVADO") : "PENDENTE");
                materiasNotas.add(materiaNota);
            }
            
            linha.put("detalhamento", materiasNotas);
            linhas.add(linha);
        }
        
        relatorio.setDados(dados);
        relatorio.setLinhas(linhas);
        
        return relatorio;
    }
    
    private Double calcularMediaGeralAluno(Long alunoId) {
        List<MatriculaModel> matriculas = matriculaRepository.findByAlunoIdWithNotas(alunoId);
        if (matriculas.isEmpty()) return 0.0;
        
        double somaMedias = 0.0;
        int concluidas = 0;
        for (MatriculaModel m : matriculas) {
            if (m.getStatus() == MatriculaModel.StatusMatricula.APROVADO ||
                    m.getStatus() == MatriculaModel.StatusMatricula.REPROVADO) {
                somaMedias += m.getMediaFinal();
                concluidas++;
            }
        }
        return concluidas == 0 ? null : somaMedias / concluidas;
    }

    public RelatorioDTO gerarRendimentoPorCurso() {
        List<CursoModel> cursos = cursoRepository.findAll();
        
        RelatorioDTO relatorio = new RelatorioDTO();
        relatorio.setTipo("RENDIMENTO_POR_CURSO");
        
        List<Map<String, Object>> linhas = new ArrayList<>();
        Map<String, Double> totais = new LinkedHashMap<>();
        
        double somaMedias = 0.0;
        int totalCursos = 0;
        
        for (CursoModel curso : cursos) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("curso", curso.getNome());
            
            // Calcular média das notas finais das matrículas aprovadas deste curso
            List<MatriculaModel> matriculasAprovadas = matriculaRepository.findByCursoId(curso.getId()).stream()
                    .filter(m -> m.getStatus() == MatriculaModel.StatusMatricula.APROVADO)
                    .collect(Collectors.toList());
            
            double soma = 0.0;
            for (MatriculaModel m : matriculasAprovadas) {
                soma += m.getMediaFinal();
            }
            double media = matriculasAprovadas.isEmpty() ? 0.0 : soma / matriculasAprovadas.size();
            linha.put("media", media);
            
            long totalAlunos = matriculaRepository.findByCursoId(curso.getId()).stream()
                    .map(MatriculaModel::getAluno)
                    .distinct()
                    .count();
            linha.put("totalAlunos", totalAlunos);
            
            linhas.add(linha);
            somaMedias += media;
            totalCursos++;
        }
        
        totais.put("mediaGeral", totalCursos > 0 ? somaMedias / totalCursos : 0.0);
        relatorio.setLinhas(linhas);
        relatorio.setTotais(totais);
        
        return relatorio;
    }

    public RelatorioDTO gerarTaxaAprovacao() {
        RelatorioDTO relatorio = new RelatorioDTO();
        relatorio.setTipo("TAXA_APROVACAO");
        
        List<MatriculaModel> todasMatriculas = matriculaRepository.findAll();
        long total = todasMatriculas.size();
        long aprovados = todasMatriculas.stream()
                .filter(m -> m.getStatus() == MatriculaModel.StatusMatricula.APROVADO)
                .count();
        
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("totalMatriculas", total);
        dados.put("totalAprovados", aprovados);
        dados.put("taxaAprovacao", total > 0 ? (aprovados * 100.0 / total) : 0.0);
        
        // Adicionar dados por curso
        List<Map<String, Object>> porCurso = new ArrayList<>();
        List<CursoModel> cursos = cursoRepository.findAll();
        for (CursoModel curso : cursos) {
            List<MatriculaModel> matriculasCurso = matriculaRepository.findByCursoId(curso.getId());
            long totalCurso = matriculasCurso.size();
            long aprovadosCurso = matriculasCurso.stream()
                    .filter(m -> m.getStatus() == MatriculaModel.StatusMatricula.APROVADO)
                    .count();
            Map<String, Object> cursoData = new LinkedHashMap<>();
            cursoData.put("curso", curso.getNome());
            cursoData.put("total", totalCurso);
            cursoData.put("aprovados", aprovadosCurso);
            cursoData.put("taxa", totalCurso > 0 ? (aprovadosCurso * 100.0 / totalCurso) : 0.0);
            porCurso.add(cursoData);
        }
        dados.put("porCurso", porCurso);
        
        relatorio.setDados(dados);
        return relatorio;
    }
}