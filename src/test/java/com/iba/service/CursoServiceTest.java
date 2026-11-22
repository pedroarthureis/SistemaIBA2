package com.iba.service;

import com.iba.dto.CursoDTO;
import com.iba.model.CursoModel;
import com.iba.model.MateriaModel;
import com.iba.repository.CursoRepository;
import com.iba.repository.MateriaRepository;
import com.iba.repository.MatriculaRepository;
import com.iba.repository.NotaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {
    @Mock CursoRepository cursoRepository;
    @Mock MateriaRepository materiaRepository;
    @Mock MatriculaRepository matriculaRepository;
    @Mock NotaRepository notaRepository;
    @InjectMocks CursoService service;

    @Test
    void devolveIdsDasMateriasDoCurso() {
        CursoModel curso = new CursoModel(); curso.setId(1L); curso.setNome("Teologia");
        MateriaModel materia = new MateriaModel(); materia.setId(7L); materia.setNome("História");
        curso.setMaterias(List.of(materia));
        when(cursoRepository.findByIdWithMaterias(1L)).thenReturn(Optional.of(curso));
        CursoDTO dto = service.findById(1L);
        assertEquals(List.of(7L), dto.getMateriasIds());
        assertEquals(List.of("História"), dto.getMateriasNomes());
    }

    @Test
    void rejeitaVinculoComMateriaInexistente() {
        CursoDTO dto = new CursoDTO(); dto.setNome("Teologia");dto.setMateriasIds(List.of(999L));
        when(materiaRepository.findAllById(any())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> service.create(dto));
        verify(cursoRepository, never()).save(any());
    }
}
