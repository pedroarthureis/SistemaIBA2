package com.iba.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matricula", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"aluno_id", "ano", "semestre"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatriculaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", nullable = false)
    private AlunoModel aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private CursoModel curso;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer semestre; // 1 ou 2

    @Column(nullable = false)
    private LocalDate dataMatricula;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private StatusMatricula status = StatusMatricula.CURSANDO;

    @OneToMany(mappedBy = "matricula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<NotaModel> notas = new ArrayList<>();

    // Associações individuais: um aluno pode cursar apenas parte da grade de seu curso.
    // Matrículas antigas não possuem linhas nessa tabela e usam a grade do curso (compatibilidade).
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "matricula_materia",
        joinColumns = @JoinColumn(name = "matricula_id"),
        inverseJoinColumns = @JoinColumn(name = "materia_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"matricula_id", "materia_id"}))
    @JsonIgnore
    private List<MateriaModel> materias = new ArrayList<>();

    public List<MateriaModel> getMateriasEfetivas() {
        return materias != null && !materias.isEmpty() ? materias :
            (curso != null && curso.getMaterias() != null ? curso.getMaterias() : List.of());
    }

    @Transient
    private Double mediaFinal;

// No método getMediaFinal() - manter cálculo normal
// No método que define aprovação, ajustar para 60

public Double getMediaFinal() {
    if (notas == null || notas.isEmpty()) return 0.0;
    
    java.util.Map<Long, java.util.List<Double>> notasPorMateria = new java.util.HashMap<>();
    for (NotaModel nota : notas) {
        notasPorMateria.computeIfAbsent(nota.getMateria().getId(), k -> new java.util.ArrayList<>())
                       .add(nota.getValor());
    }
    
    double somaMedias = 0.0;
    for (java.util.List<Double> notasMateria : notasPorMateria.values()) {
        double mediaMateria = notasMateria.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        somaMedias += mediaMateria;
    }
    
    return notasPorMateria.isEmpty() ? 0.0 : somaMedias / notasPorMateria.size();
}
    
    public enum StatusMatricula {
        CURSANDO, APROVADO, REPROVADO, TRANCADA
    }
}