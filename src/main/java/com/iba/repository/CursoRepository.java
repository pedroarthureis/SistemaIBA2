package com.iba.repository;

import com.iba.model.CursoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<CursoModel, Long> {
    
    Optional<CursoModel> findByNome(String nome);
    boolean existsByNome(String nome);
    
    // CORRIGIR: Usar LEFT JOIN FETCH para carregar as matérias
    @Query("SELECT DISTINCT c FROM CursoModel c LEFT JOIN FETCH c.materias")
    List<CursoModel> findAllWithMaterias();
    
    // Este método também pode ser útil
    @Query("SELECT c FROM CursoModel c LEFT JOIN FETCH c.materias WHERE c.id = :id")
    Optional<CursoModel> findByIdWithMaterias(@Param("id") Long id);
    
    @Query("SELECT c.id, c.nome, COUNT(m) FROM CursoModel c LEFT JOIN c.materias m GROUP BY c.id, c.nome")
    List<Object[]> countMateriasByCurso();
}