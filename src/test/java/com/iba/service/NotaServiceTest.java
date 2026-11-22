package com.iba.service;

import com.iba.dto.NotaDTO;
import com.iba.model.AlunoModel;
import com.iba.model.CursoModel;
import com.iba.model.MateriaModel;
import com.iba.model.MatriculaModel;
import com.iba.model.NotaModel;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import com.iba.repository.MatriculaRepository;
import com.iba.repository.MateriaRepository;
import com.iba.repository.NotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotaServiceTest {
    @Mock NotaRepository notaRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock MateriaRepository materiaRepository;
    @InjectMocks NotaService service;

    private NotaDTO entrada(Double valor, Integer ciclo) {
        NotaDTO dto = new NotaDTO();
        dto.setMatriculaId(1L);
        dto.setMateriaId(2L);
        dto.setCiclo(ciclo);
        dto.setValor(valor);
        return dto;
    }

    @Test
    void naoAceitaNotaNegativa() {
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(-0.1, 1)));
        verifyNoInteractions(notaRepository, matriculaRepository, materiaRepository);
    }

    @Test
    void naoAceitaNotaMaiorQueCem() {
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(100.1, 1)));
        verifyNoInteractions(notaRepository, matriculaRepository, materiaRepository);
    }

    @Test
    void naoAceitaNotaNulaOuNaoFinita() {
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(null, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(Double.NaN, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(Double.POSITIVE_INFINITY, 1)));
        verifyNoInteractions(notaRepository, matriculaRepository, materiaRepository);
    }

    @Test
    void naoAceitaCicloForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(75.0, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(75.0, 4)));
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(75.0, null)));
        verifyNoInteractions(notaRepository, matriculaRepository, materiaRepository);
    }

    @Test
    void naoAceitaReferenciasAusentes() {
        NotaDTO dto = entrada(60.0, 1);
        dto.setMateriaId(null);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.save(dto))
            .getMessage().contains("obrigatórias"));
    }

    private MatriculaModel matriculaComMateria(Long idMateria) {
        AlunoModel aluno = new AlunoModel();
        aluno.setId(10L); aluno.setNome("Aluno teste");
        MateriaModel materia = new MateriaModel();
        materia.setId(idMateria); materia.setNome("Matéria teste");
        CursoModel curso = new CursoModel();
        curso.setId(20L); curso.setNome("Teologia"); curso.setMaterias(new ArrayList<>(List.of(materia)));
        MatriculaModel matricula = new MatriculaModel();
        matricula.setId(1L); matricula.setAluno(aluno);
        matricula.setCurso(curso); matricula.setStatus(MatriculaModel.StatusMatricula.CURSANDO);
        return matricula;
    }

    @Test
    void impedeNotaDeMateriaNaoVinculada() {
        MatriculaModel matricula = matriculaComMateria(99L);
        MateriaModel materia = new MateriaModel(); materia.setId(2L);
        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
        when(materiaRepository.findById(2L)).thenReturn(Optional.of(materia));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.save(entrada(80.0, 1)))
                .getMessage().contains("não está vinculada"));
        verify(notaRepository, never()).saveAndFlush(any());
    }

    private void prepararLancamento(MatriculaModel matricula, List<NotaModel> notasAposSave) {
        MateriaModel materia = matricula.getCurso().getMaterias().get(0);
        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
        when(materiaRepository.findById(2L)).thenReturn(Optional.of(materia));
        when(notaRepository.findByMatriculaIdAndMateriaIdAndCiclo(1L,2L,3)).thenReturn(Optional.empty());
        when(notaRepository.saveAndFlush(any(NotaModel.class))).thenAnswer(i -> i.getArgument(0));
        when(notaRepository.findByMatriculaId(1L)).thenReturn(notasAposSave);
    }

    private NotaModel nota(MatriculaModel matricula, int ciclo, double valor) {
        NotaModel nota = new NotaModel();
        nota.setMatricula(matricula);
        nota.setMateria(matricula.getCurso().getMaterias().get(0));
        nota.setCiclo(ciclo); nota.setValor(valor);
        return nota;
    }

    @Test
    void soAprovaQuandoTodosOsCiclosEstaoCompletos() {
        MatriculaModel matricula = matriculaComMateria(2L);
        prepararLancamento(matricula, List.of(nota(matricula,1,80),nota(matricula,3,70)));
        service.save(entrada(70.0,3));
        assertEquals(MatriculaModel.StatusMatricula.CURSANDO, matricula.getStatus());
    }

    @Test
    void concluiMatriculaComMediaSuficiente() {
        MatriculaModel matricula = matriculaComMateria(2L);
        prepararLancamento(matricula, List.of(nota(matricula,1,70),nota(matricula,2,80),nota(matricula,3,70)));
        service.save(entrada(70.0,3));
        assertEquals(MatriculaModel.StatusMatricula.APROVADO, matricula.getStatus());
    }

    @Test
    void naoAprovaQuandoMediaDaMateriaEInsuficiente() {
        MatriculaModel matricula = matriculaComMateria(2L);
        prepararLancamento(matricula, List.of(nota(matricula,1,20),nota(matricula,2,30),nota(matricula,3,50)));
        service.save(entrada(50.0,3));
        assertEquals(MatriculaModel.StatusMatricula.REPROVADO, matricula.getStatus());
    }


    @Test
    void impedeNotaParaMateriaDoCursoNaoEscolhidaPeloAluno() {
        MatriculaModel matricula = matriculaComMateria(2L);
        MateriaModel materiaNaoEscolhida = new MateriaModel(); materiaNaoEscolhida.setId(3L);
        matricula.getCurso().setMaterias(new ArrayList<>(List.of(matricula.getCurso().getMaterias().get(0), materiaNaoEscolhida)));
        matricula.setMaterias(new ArrayList<>(List.of(materiaNaoEscolhida)));
        when(matriculaRepository.findById(1L)).thenReturn(Optional.of(matricula));
        when(materiaRepository.findById(2L)).thenReturn(Optional.of(matricula.getCurso().getMaterias().get(0)));
        assertThrows(IllegalArgumentException.class, () -> service.save(entrada(75.0,1)));
        verify(notaRepository, never()).saveAndFlush(any());
    }

    @Test
    void concluiComNotasDaMateriaSelecionadaSemExigirTodaAGrade() {
        MatriculaModel matricula = matriculaComMateria(2L);
        MateriaModel adicional = new MateriaModel(); adicional.setId(3L);
        matricula.getCurso().setMaterias(new ArrayList<>(List.of(matricula.getCurso().getMaterias().get(0),adicional)));
        matricula.setMaterias(new ArrayList<>(List.of(matricula.getCurso().getMaterias().get(0))));
        prepararLancamento(matricula, List.of(nota(matricula,1,85),nota(matricula,2,80),nota(matricula,3,95)));
        service.save(entrada(95.0,3));
        assertEquals(MatriculaModel.StatusMatricula.APROVADO, matricula.getStatus());
    }
}
