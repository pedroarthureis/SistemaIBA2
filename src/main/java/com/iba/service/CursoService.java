package com.iba.service;

import com.iba.dto.CursoDTO;
import com.iba.model.CursoModel;
import com.iba.model.MateriaModel;
import com.iba.repository.CursoRepository;
import com.iba.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final com.iba.repository.MatriculaRepository matriculaRepository;
    private final com.iba.repository.NotaRepository notaRepository;

    private CursoDTO toDTO(CursoModel curso) {
        CursoDTO dto = new CursoDTO();
        dto.setId(curso.getId());
        dto.setNome(curso.getNome());
        
        if (curso.getMaterias() != null && !curso.getMaterias().isEmpty()) {
            // Preencher nomes das matérias para exibição
            dto.setMateriasNomes(curso.getMaterias().stream()
                    .map(MateriaModel::getNome)
                    .collect(Collectors.toList()));
            
            // ← ADICIONAR ESTA LINHA: Preencher os IDs das matérias
            dto.setMateriasIds(curso.getMaterias().stream()
                    .map(MateriaModel::getId)
                    .collect(Collectors.toList()));
        }
        
        dto.setQuantidadeAlunos(0); // Será implementado depois
        
        return dto;
    }

    @Transactional(readOnly = true)
    public List<CursoDTO> findAll() {
        return cursoRepository.findAllWithMaterias().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CursoDTO findById(Long id) {
        CursoModel curso = cursoRepository.findByIdWithMaterias(id)
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));
        return toDTO(curso);
    }

    @Transactional
    public CursoDTO create(CursoDTO cursoDTO) {
        String nome = validarNome(cursoDTO);
        if (cursoRepository.existsByNome(nome)) {
            throw new RuntimeException("Já existe um curso com este nome");
        }

        CursoModel curso = new CursoModel();
        curso.setNome(nome);
        curso.setMaterias(buscarMaterias(cursoDTO.getMateriasIds()));

        CursoModel saved = cursoRepository.save(curso);
        return toDTO(saved);
    }

    @Transactional
    public CursoDTO update(Long id, CursoDTO cursoDTO) {
        CursoModel curso = cursoRepository.findByIdWithMaterias(id)
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));

        String nome = validarNome(cursoDTO);
        cursoRepository.findByNome(nome).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new IllegalArgumentException("Já existe outro curso com este nome");
            }
        });
        curso.setNome(nome);
        if (cursoDTO.getMateriasIds() != null) {
            List<MateriaModel> novasMaterias = buscarMaterias(cursoDTO.getMateriasIds());
            Set<Long> idsNovos = novasMaterias.stream().map(MateriaModel::getId).collect(Collectors.toSet());
            Set<Long> idsAntigos = curso.getMaterias().stream().map(MateriaModel::getId).collect(Collectors.toSet());
            if (!idsNovos.equals(idsAntigos) && matriculaRepository.findByCursoId(id).stream()
                    .anyMatch(m -> m.getStatus() == com.iba.model.MatriculaModel.StatusMatricula.APROVADO
                            || m.getStatus() == com.iba.model.MatriculaModel.StatusMatricula.REPROVADO)) {
                throw new IllegalArgumentException("Este curso possui matrículas concluídas. Não altere a grade histórica; crie um novo curso ou versão curricular.");
            }
            for (MateriaModel materiaAntiga : curso.getMaterias()) {
                if (!idsNovos.contains(materiaAntiga.getId())) {
                    if (matriculaRepository.findByCursoId(id).stream()
                            .anyMatch(m -> m.getMaterias() == null || m.getMaterias().isEmpty())) {
                        throw new IllegalArgumentException("Matéria pertence à grade de matrículas antigas. Preserve a grade histórica.");
                    }
                    // Uma matrícula individual já pode conter esta matéria sem nota.
                    if (matriculaRepository.findByMateriaSelecionada(materiaAntiga.getId()).stream()
                            .anyMatch(m -> m.getCurso().getId().equals(id))) {
                        throw new IllegalArgumentException("Matéria vinculada a matrícula individual. Remova-a das matrículas em andamento antes de alterar o curso.");
                    }
                    // Não permite apagar vínculo que mantém notas acadêmicas existentes.
                    for (com.iba.model.MatriculaModel matricula : matriculaRepository.findByCursoId(id)) {
                        if (notaRepository.findByMatriculaId(matricula.getId()).stream()
                                .anyMatch(nota -> nota.getMateria().getId().equals(materiaAntiga.getId()))) {
                            throw new IllegalArgumentException("Não é possível desvincular matéria com notas já lançadas neste curso");
                        }
                    }
                }
            }
            curso.setMaterias(novasMaterias);
        }

        CursoModel saved = cursoRepository.save(curso);
        return toDTO(saved);
    }

    private String validarNome(CursoDTO dto) {
        if (dto == null || dto.getNome() == null || dto.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do curso é obrigatório");
        }
        return dto.getNome().trim();
    }

    private List<MateriaModel> buscarMaterias(List<Long> ids) {
        if (ids == null) return new ArrayList<>();
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Identificador de matéria inválido");
        }
        Set<Long> distintos = new LinkedHashSet<>(ids);
        List<MateriaModel> materias = materiaRepository.findAllById(distintos);
        if (materias.size() != distintos.size()) {
            throw new IllegalArgumentException("Uma ou mais matérias selecionadas não existem");
        }
        return materias;
    }

    @Transactional
    public void delete(Long id) {
        cursoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Curso não encontrado"));
        if (!matriculaRepository.findByCursoId(id).isEmpty()) {
            throw new IllegalArgumentException("Curso possui matrículas. Não é possível excluí-lo.");
        }
        cursoRepository.deleteById(id);
    }
}