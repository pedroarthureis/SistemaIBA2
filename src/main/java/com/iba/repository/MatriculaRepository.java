package com.iba.repository;

import com.iba.model.MatriculaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<MatriculaModel, Long> {
    
    List<MatriculaModel> findByAlunoId(Long alunoId);
    
    List<MatriculaModel> findByCursoId(Long cursoId);

    @Query("SELECT DISTINCT m FROM MatriculaModel m JOIN m.materias mm WHERE mm.id = :materiaId")
    List<MatriculaModel> findByMateriaSelecionada(@Param("materiaId") Long materiaId);
    
    Optional<MatriculaModel> findByAlunoIdAndAnoAndSemestre(Long alunoId, Integer ano, Integer semestre);
    
    @Query("SELECT m FROM MatriculaModel m WHERE m.curso.id = :cursoId AND m.ano = :ano AND m.semestre = :semestre")
    List<MatriculaModel> findByCursoAndPeriodo(@Param("cursoId") Long cursoId, @Param("ano") Integer ano, @Param("semestre") Integer semestre);
    
    @Query("SELECT m FROM MatriculaModel m WHERE m.curso.id = :cursoId AND m.status = 'CURSANDO'")
    List<MatriculaModel> findAtivasByCurso(@Param("cursoId") Long cursoId);
    
    @Query("SELECT m FROM MatriculaModel m LEFT JOIN FETCH m.notas WHERE m.aluno.id = :alunoId")
    List<MatriculaModel> findByAlunoIdWithNotas(@Param("alunoId") Long alunoId);
    
    @Query("SELECT COUNT(DISTINCT m.aluno.id) FROM MatriculaModel m WHERE m.ano = :ano AND m.semestre = :semestre")
    Long countAlunosMatriculados(@Param("ano") Integer ano, @Param("semestre") Integer semestre);
    
    // Método para calcular média das notas finais das matrículas aprovadas
    @Query("SELECT AVG((SELECT AVG(n.valor) FROM NotaModel n WHERE n.matricula.id = m.id)) FROM MatriculaModel m WHERE m.curso.id = :cursoId AND m.status = 'APROVADO'")
    Double calcularMediaAprovadosPorCurso(@Param("cursoId") Long cursoId);
}