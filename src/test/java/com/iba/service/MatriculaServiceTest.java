package com.iba.service;

import com.iba.dto.MatriculaDTO;
import com.iba.model.AlunoModel;
import com.iba.model.CursoModel;
import com.iba.model.MateriaModel;
import com.iba.model.MatriculaModel;
import com.iba.model.NotaModel;
import com.iba.repository.MateriaRepository;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.iba.repository.AlunoRepository;
import com.iba.repository.CursoRepository;
import com.iba.repository.MatriculaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {
    @Mock MatriculaRepository matriculaRepository;
    @Mock AlunoRepository alunoRepository;
    @Mock CursoRepository cursoRepository;
    @Mock MateriaRepository materiaRepository;
    @Mock NotaService notaService;
    @InjectMocks MatriculaService service;

    private MatriculaDTO entrada(Integer ano, Integer semestre) {
        MatriculaDTO dto = new MatriculaDTO();
        dto.setAlunoId(1L);
        dto.setCursoId(2L);
        dto.setAno(ano);
        dto.setSemestre(semestre);
        return dto;
    }

    @Test
    void impedeMatriculaComSemestreInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.create(entrada(2026, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.create(entrada(2026, 3)));
        verifyNoInteractions(matriculaRepository, alunoRepository, cursoRepository);
    }

    @Test
    void impedeMatriculaComAnoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.create(entrada(null, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.create(entrada(1999, 1)));
        verifyNoInteractions(matriculaRepository, alunoRepository, cursoRepository);
    }

    private MatriculaModel matriculaComGrade() {
        AlunoModel aluno = new AlunoModel(); aluno.setId(1L); aluno.setNome("João"); aluno.setMatricula("IBA-1");
        MateriaModel a = new MateriaModel(); a.setId(8L); a.setNome("A");
        MateriaModel b = new MateriaModel(); b.setId(9L); b.setNome("B");
        CursoModel curso = new CursoModel(); curso.setId(2L); curso.setNome("Teologia");
        curso.setMaterias(List.of(a,b));
        MatriculaModel mat = new MatriculaModel(); mat.setId(3L); mat.setAluno(aluno);
        mat.setCurso(curso); mat.setAno(2026); mat.setSemestre(2);
        mat.setStatus(MatriculaModel.StatusMatricula.CURSANDO);
        mat.setNotas(new ArrayList<>());
        return mat;
    }

    @Test
    void permiteSelecionarSomenteParteDasMateriasDoCurso() {
        MatriculaModel mat = matriculaComGrade();
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(mat.getAluno()));
        when(cursoRepository.findById(2L)).thenReturn(Optional.of(mat.getCurso()));
        when(matriculaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MatriculaDTO dto = entrada(2026, 2); dto.setMateriasIds(List.of(8L));
        MatriculaDTO criado = service.create(dto);
        assertEquals(List.of(8L), criado.getMateriasIds());
        assertEquals(List.of("A"), criado.getMateriasNomes());
    }

    @Test
    void rejeitaMateriaQueNaoPertenceAoCurso() {
        MatriculaModel mat = matriculaComGrade();
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(mat.getAluno()));
        when(cursoRepository.findById(2L)).thenReturn(Optional.of(mat.getCurso()));
        MatriculaDTO dto = entrada(2026, 2); dto.setMateriasIds(List.of(77L));
        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void impedeRetirarDisciplinaComNota() {
        MatriculaModel mat = matriculaComGrade();
        mat.setMaterias(new ArrayList<>(mat.getCurso().getMaterias()));
        NotaModel nota = new NotaModel(); nota.setMateria(mat.getCurso().getMaterias().get(1));
        mat.setNotas(new ArrayList<>(List.of(nota)));
        when(matriculaRepository.findById(3L)).thenReturn(Optional.of(mat));
        assertThrows(IllegalArgumentException.class, () -> service.updateMaterias(3L, List.of(8L)));
        verify(matriculaRepository, never()).saveAndFlush(any());
    }

    @Test
    void impedeStatusAprovadoManual() {
        MatriculaModel mat = matriculaComGrade();
        when(matriculaRepository.findById(3L)).thenReturn(Optional.of(mat));
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(3L,"APROVADO"));
        verify(matriculaRepository, never()).save(any());
    }
}
