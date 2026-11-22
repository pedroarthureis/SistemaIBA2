package com.iba.service;

import com.iba.dto.MateriaDTO;
import com.iba.model.MateriaModel;
import com.iba.repository.MateriaRepository;
import com.iba.repository.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MateriaService {

    private final MateriaRepository materiaRepository;
    private final NotaRepository notaRepository;
    private final com.iba.repository.MatriculaRepository matriculaRepository;

    private MateriaDTO toDTO(MateriaModel materia) {
        MateriaDTO dto = new MateriaDTO();
        dto.setId(materia.getId());  // ID é o código
        dto.setNome(materia.getNome());
        dto.setQuantidadeCursos(materiaRepository.countCursosByMateriaId(materia.getId()));
        dto.setMediaGeralNotas(notaRepository.calcularMediaPorMateria(materia.getId()));
        return dto;
    }

    public List<MateriaDTO> findAll() {
        return materiaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public MateriaDTO findById(Long id) {
        MateriaModel materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matéria não encontrada"));
        return toDTO(materia);
    }

    @Transactional
    public MateriaDTO create(MateriaDTO materiaDTO) {
        validarNome(materiaDTO);
        
        MateriaModel materia = new MateriaModel();
        materia.setNome(materiaDTO.getNome().trim());

        MateriaModel saved = materiaRepository.save(materia);
        return toDTO(saved);
    }

    @Transactional
    public MateriaDTO update(Long id, MateriaDTO materiaDTO) {
        validarNome(materiaDTO);
        MateriaModel materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matéria não encontrada"));

        materia.setNome(materiaDTO.getNome().trim());

        MateriaModel saved = materiaRepository.save(materia);
        return toDTO(saved);
    }

    private void validarNome(MateriaDTO dto) {
        if (dto == null || dto.getNome() == null || dto.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da matéria é obrigatório");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!materiaRepository.existsById(id)) {
            throw new RuntimeException("Matéria não encontrada");
        }
        if (!notaRepository.findByMateriaId(id).isEmpty()) {
            throw new IllegalArgumentException("Não é possível excluir matéria com notas registradas");
        }
        if (!matriculaRepository.findByMateriaSelecionada(id).isEmpty()) {
            throw new IllegalArgumentException("Matéria selecionada em matrícula; não é possível excluir");
        }
        if (materiaRepository.countCursosByMateriaId(id) > 0) {
            throw new IllegalArgumentException("Desvincule a matéria de todos os cursos antes de excluí-la");
        }
        materiaRepository.deleteById(id);
    }
}