package com.iba.service;

import com.iba.audit.AuditEvents;

import com.iba.dto.NotaDTO;
import com.iba.model.MatriculaModel;
import com.iba.model.MateriaModel;
import com.iba.model.NotaModel;
import com.iba.repository.MatriculaRepository;
import com.iba.repository.MateriaRepository;
import com.iba.repository.NotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotaService {

    private final NotaRepository notaRepository;
    private final MatriculaRepository matriculaRepository;
    private final MateriaRepository materiaRepository;

    private NotaDTO toDTO(NotaModel nota) {
        NotaDTO dto = new NotaDTO();
        dto.setId(nota.getId());
        dto.setValor(nota.getValor());
        dto.setCiclo(nota.getCiclo());
        dto.setMatriculaId(nota.getMatricula().getId());
        dto.setAlunoId(nota.getMatricula().getAluno().getId());
        dto.setAlunoNome(nota.getMatricula().getAluno().getNome());
        dto.setMateriaId(nota.getMateria().getId());
        dto.setMateriaNome(nota.getMateria().getNome());
        dto.setDataLancamento(nota.getDataLancamento());
        return dto;
    }

    public List<NotaDTO> findAll() {
        return notaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<NotaDTO> findByMatriculaId(Long matriculaId) {
        return notaRepository.findByMatriculaId(matriculaId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<NotaDTO> findByAlunoId(Long alunoId) {
        List<MatriculaModel> matriculas = matriculaRepository.findByAlunoId(alunoId);
        return matriculas.stream()
                .flatMap(m -> m.getNotas().stream())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<NotaDTO> findByMateriaId(Long materiaId) {
        List<NotaModel> notas = notaRepository.findByMateriaId(materiaId);
        return notas.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotaDTO save(NotaDTO notaDTO) {
        // Rejeitar dados invalidos antes de consultar ou alterar registros.
        if (notaDTO == null || notaDTO.getMatriculaId() == null || notaDTO.getMateriaId() == null) {
            throw new IllegalArgumentException("Matrícula e matéria são obrigatórias");
        }
        if (notaDTO.getCiclo() == null || notaDTO.getCiclo() < 1 || notaDTO.getCiclo() > 3) {
            throw new IllegalArgumentException("Ciclo deve estar entre 1 e 3");
        }
        if (notaDTO.getValor() == null || !Double.isFinite(notaDTO.getValor())
                || notaDTO.getValor() < 0 || notaDTO.getValor() > 100) {
            throw new IllegalArgumentException("Nota deve estar entre 0 e 100");
        }

        MatriculaModel matricula = matriculaRepository.findById(notaDTO.getMatriculaId())
                .orElseThrow(() -> new RuntimeException("Matrícula não encontrada"));
        
        MateriaModel materia = materiaRepository.findById(notaDTO.getMateriaId())
                .orElseThrow(() -> new RuntimeException("Matéria não encontrada"));

        // A matéria precisa estar vinculada ao curso da matrícula. Validar no servidor
        // impede notas órfãs e não depende de uma seleção correta no navegador.
        if (matricula.getMateriasEfetivas() == null ||
                matricula.getMateriasEfetivas().stream().noneMatch(m -> m.getId().equals(materia.getId()))) {
            throw new IllegalArgumentException("Esta matéria não está vinculada à matrícula deste aluno. Selecione-a nas matérias da matrícula.");
        }
        if (matricula.getStatus() == MatriculaModel.StatusMatricula.TRANCADA) {
            throw new IllegalArgumentException("Não é possível lançar notas para matrícula trancada");
        }

        NotaModel nota = notaRepository.findByMatriculaIdAndMateriaIdAndCiclo(
                matricula.getId(), materia.getId(), notaDTO.getCiclo())
                .orElse(new NotaModel());

        nota.setValor(notaDTO.getValor());
        nota.setCiclo(notaDTO.getCiclo());
        nota.setMatricula(matricula);
        nota.setMateria(materia);
        nota.setDataLancamento(LocalDate.now());

        NotaModel saved = notaRepository.saveAndFlush(nota);
        
        // Verificar se todas as notas do período foram lançadas e atualizar status
        recalcularStatus(matricula);
        AuditEvents.afterCommit("UPSERT", "nota", saved.getId());
        return toDTO(saved);
    }
    
    // Consulta as notas atualizadas após o save, evitando coleções JPA desatualizadas.
    public void recalcularStatus(MatriculaModel matricula) {
        if (matricula.getStatus() == MatriculaModel.StatusMatricula.TRANCADA) return;
        List<MateriaModel> materias = matricula.getMateriasEfetivas();
        List<NotaModel> notas = notaRepository.findByMatriculaId(matricula.getId());
        if (materias == null || materias.isEmpty()) {
            matricula.setStatus(MatriculaModel.StatusMatricula.CURSANDO);
        } else {
            boolean completo = true;
            boolean todasAprovadas = true;
            for (MateriaModel materia : materias) {
                java.util.Map<Integer, Double> ciclos = new java.util.HashMap<>();
                for (NotaModel nota : notas) {
                    if (nota.getMateria().getId().equals(materia.getId()) &&
                            nota.getCiclo() >= 1 && nota.getCiclo() <= 3) {
                        ciclos.put(nota.getCiclo(), nota.getValor());
                    }
                }
                if (ciclos.size() < 3) {
                    completo = false;
                } else if (ciclos.values().stream().mapToDouble(Double::doubleValue).average().orElse(0) < 60.0) {
                    todasAprovadas = false;
                }
            }
            matricula.setStatus(!completo ? MatriculaModel.StatusMatricula.CURSANDO :
                    (todasAprovadas ? MatriculaModel.StatusMatricula.APROVADO : MatriculaModel.StatusMatricula.REPROVADO));
        }
        matriculaRepository.save(matricula);
    }

    @Transactional
    public void delete(Long id) {
        NotaModel nota = notaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada"));
        MatriculaModel matricula = nota.getMatricula();
        if (matricula.getStatus() == MatriculaModel.StatusMatricula.TRANCADA) {
            throw new IllegalArgumentException("Não é possível modificar notas de matrícula trancada");
        }
        notaRepository.delete(nota);
        notaRepository.flush();
        recalcularStatus(matricula);
        AuditEvents.afterCommit("DELETE", "nota", id);
    }
}