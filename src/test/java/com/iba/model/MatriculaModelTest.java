package com.iba.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatriculaModelTest {
    @Test
    void semNotasMediaEhZero() {
        MatriculaModel matricula = new MatriculaModel();
        assertEquals(0.0, matricula.getMediaFinal());
    }

    @Test
    void calculaMediaDasMediasPorMateria() {
        MateriaModel materiaA = new MateriaModel();
        materiaA.setId(10L);
        MateriaModel materiaB = new MateriaModel();
        materiaB.setId(20L);
        NotaModel n1 = nota(materiaA, 80.0);
        NotaModel n2 = nota(materiaA, 100.0);
        NotaModel n3 = nota(materiaB, 60.0);
        MatriculaModel matricula = new MatriculaModel();
        matricula.setNotas(List.of(n1, n2, n3));
        assertEquals(75.0, matricula.getMediaFinal(), 0.001);
    }

    private NotaModel nota(MateriaModel materia, Double valor) {
        NotaModel nota = new NotaModel();
        nota.setMateria(materia);
        nota.setValor(valor);
        return nota;
    }
}
