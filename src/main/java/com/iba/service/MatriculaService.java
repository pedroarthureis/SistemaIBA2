package com.iba.service;

import com.iba.audit.AuditEvents;

import com.iba.dto.MatriculaDTO;
import com.iba.dto.NotaDTO;
import com.iba.model.AlunoModel;
import com.iba.model.CursoModel;
import com.iba.model.MatriculaModel;
import com.iba.model.NotaModel;
import com.iba.repository.AlunoRepository;
import com.iba.repository.CursoRepository;
import com.iba.repository.MatriculaRepository;
import com.iba.repository.MateriaRepository;
import com.iba.model.MateriaModel;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final AlunoRepository alunoRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final NotaService notaService;

    private MatriculaDTO toDTO(MatriculaModel matricula) {
        MatriculaDTO dto = new MatriculaDTO();
        dto.setId(matricula.getId());
        dto.setAlunoId(matricula.getAluno().getId());
        dto.setAlunoNome(matricula.getAluno().getNome());
        dto.setAlunoMatricula(matricula.getAluno().getMatricula());
        dto.setCursoId(matricula.getCurso().getId());
        dto.setCursoNome(matricula.getCurso().getNome());
        dto.setAno(matricula.getAno());
        dto.setSemestre(matricula.getSemestre());
        dto.setDataMatricula(matricula.getDataMatricula());
        dto.setStatus(matricula.getStatus().toString());
        dto.setMediaFinal(matricula.getMediaFinal());
        List<MateriaModel> materias = matricula.getMateriasEfetivas();
        dto.setMateriasIds(materias.stream().map(MateriaModel::getId).toList());
        dto.setMateriasNomes(materias.stream().map(MateriaModel::getNome).toList());
        
        if (matricula.getNotas() != null && !matricula.getNotas().isEmpty()) {
            dto.setNotas(matricula.getNotas().stream()
                .map(this::notaToDTO)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private NotaDTO notaToDTO(NotaModel nota) {
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

    @Transactional(readOnly = true)
    public List<MatriculaDTO> findAll() {
        return matriculaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatriculaDTO> findByAlunoId(Long alunoId) {
        return matriculaRepository.findByAlunoId(alunoId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatriculaDTO findById(Long id) {
        MatriculaModel matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matrícula não encontrada"));
        return toDTO(matricula);
    }

    @Transactional(readOnly = true)
    public List<MatriculaDTO> findByCursoAndPeriodo(Long cursoId, Integer ano, Integer semestre) {
        return matriculaRepository.findByCursoAndPeriodo(cursoId, ano, semestre).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatriculaDTO> findAtivasByCurso(Long cursoId) {
        return matriculaRepository.findAtivasByCurso(cursoId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MatriculaDTO create(MatriculaDTO matriculaDTO) {
        if (matriculaDTO == null || matriculaDTO.getAlunoId() == null || matriculaDTO.getCursoId() == null) {
            throw new IllegalArgumentException("Aluno e curso são obrigatórios");
        }
        if (matriculaDTO.getAno() == null || matriculaDTO.getAno() < 2000 || matriculaDTO.getAno() > 2100) {
            throw new IllegalArgumentException("Ano da matrícula inválido");
        }
        if (matriculaDTO.getSemestre() == null || (matriculaDTO.getSemestre() != 1 && matriculaDTO.getSemestre() != 2)) {
            throw new IllegalArgumentException("Semestre deve ser 1 ou 2");
        }

        AlunoModel aluno = alunoRepository.findById(matriculaDTO.getAlunoId())
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
        
        CursoModel curso = cursoRepository.findById(matriculaDTO.getCursoId())
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));
        
        // Verificar se já existe matrícula para este aluno no mesmo ano/semestre
        if (matriculaRepository.findByAlunoIdAndAnoAndSemestre(
                aluno.getId(), matriculaDTO.getAno(), matriculaDTO.getSemestre()).isPresent()) {
            throw new RuntimeException("Aluno já possui matrícula neste período");
        }
        
        MatriculaModel matricula = new MatriculaModel();
        matricula.setAluno(aluno);
        matricula.setCurso(curso);
        matricula.setAno(matriculaDTO.getAno());
        matricula.setSemestre(matriculaDTO.getSemestre());
        matricula.setDataMatricula(LocalDate.now());
        matricula.setCreatedAt(LocalDateTime.now());
        matricula.setStatus(MatriculaModel.StatusMatricula.CURSANDO);
        matricula.setMaterias(new ArrayList<>(validarMateriasCurso(curso, matriculaDTO.getMateriasIds())));

        MatriculaModel saved = matriculaRepository.save(matricula);
        AuditEvents.afterCommit("CREATE", "matricula", saved.getId());
        return toDTO(saved);
    }

    /** Seleciona apenas disciplinas que existem na grade deste curso. Sem seleção explícita,
     * matrículas criadas pela API antiga recebem a grade inteira. */
    private List<MateriaModel> validarMateriasCurso(CursoModel curso, List<Long> selecionadas) {
        List<MateriaModel> grade = curso.getMaterias();
        List<Long> ids = selecionadas == null
                ? grade.stream().map(MateriaModel::getId).toList() : selecionadas;
        if (ids.isEmpty() || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Selecione pelo menos uma matéria válida para a matrícula");
        }
        Set<Long> distintos = new LinkedHashSet<>(ids);
        Set<Long> gradeIds = grade.stream().map(MateriaModel::getId).collect(java.util.stream.Collectors.toSet());
        if (!gradeIds.containsAll(distintos)) {
            throw new IllegalArgumentException("A matéria escolhida não pertence ao curso desta matrícula");
        }
        // Usar as entidades já carregadas, evitando referências inexistentes.
        return grade.stream().filter(m -> distintos.contains(m.getId())).toList();
    }

    @Transactional
    public MatriculaDTO updateMaterias(Long id, List<Long> materiasIds) {
        MatriculaModel matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matrícula não encontrada"));
        if (matricula.getStatus() != MatriculaModel.StatusMatricula.CURSANDO) {
            throw new IllegalArgumentException("A grade só pode ser alterada em matrículas em andamento");
        }
        List<MateriaModel> novaGrade = validarMateriasCurso(matricula.getCurso(), materiasIds);
        Set<Long> novaIds = novaGrade.stream().map(MateriaModel::getId)
                .collect(java.util.stream.Collectors.toSet());
        boolean temNotasNaMateriaRetirada = matricula.getNotas().stream()
                .anyMatch(n -> !novaIds.contains(n.getMateria().getId()));
        if (temNotasNaMateriaRetirada) {
            throw new IllegalArgumentException("Não é possível retirar matéria com nota lançada. Corrija as notas antes.");
        }
        matricula.setMaterias(new ArrayList<>(novaGrade));
        MatriculaModel salvo = matriculaRepository.saveAndFlush(matricula);
        notaService.recalcularStatus(salvo);
        AuditEvents.afterCommit("UPDATE_MATERIAS", "matricula", salvo.getId());
        return toDTO(salvo);
    }

    @Transactional
    public MatriculaDTO updateStatus(Long id, String status) {
        MatriculaModel matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matrícula não encontrada"));
        if (!"TRANCADA".equals(status) && !"CURSANDO".equals(status)) {
            throw new IllegalArgumentException("Status acadêmico é calculado pelas notas. Só é permitido trancar ou reabrir a matrícula.");
        }
        if (matricula.getStatus() == MatriculaModel.StatusMatricula.APROVADO ||
                matricula.getStatus() == MatriculaModel.StatusMatricula.REPROVADO) {
            throw new IllegalArgumentException("Matrícula concluída: não é possível alterar o status manualmente");
        }
        if ("TRANCADA".equals(status)) {
            matricula.setStatus(MatriculaModel.StatusMatricula.TRANCADA);
        } else {
            matricula.setStatus(MatriculaModel.StatusMatricula.CURSANDO);
            notaService.recalcularStatus(matricula);
        }
        MatriculaModel saved = matriculaRepository.save(matricula);
        AuditEvents.afterCommit("UPDATE_STATUS", "matricula", saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        MatriculaModel matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Matrícula não encontrada"));
        
        // Verificar se tem notas lançadas
        if (matricula.getNotas() != null && !matricula.getNotas().isEmpty()) {
            throw new RuntimeException("Não é possível excluir matrícula com notas já lançadas");
        }
        
        matriculaRepository.deleteById(id);
        AuditEvents.afterCommit("DELETE", "matricula", id);
    }
}