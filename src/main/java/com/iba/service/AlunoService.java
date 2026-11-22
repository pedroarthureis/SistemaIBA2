package com.iba.service;

import com.iba.dto.AlunoDTO;
import com.iba.dto.CursoDTO;
import com.iba.model.AlunoModel;
import com.iba.model.MatriculaModel;  // ← ADICIONAR ESTE IMPORT
import com.iba.repository.AlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    // private final CursoRepository cursoRepository; // REMOVER - não precisa mais

    private AlunoDTO toDTO(AlunoModel aluno) {
        AlunoDTO dto = new AlunoDTO();
        dto.setId(aluno.getId());
        dto.setMatricula(aluno.getMatricula());
        dto.setNome(aluno.getNome());
        dto.setCpf(aluno.getCpf());
        dto.setRg(aluno.getRg());
        dto.setEmail(aluno.getEmail());
        dto.setTelefone(aluno.getTelefone());
        dto.setEndereco(aluno.getEndereco());
        dto.setCidade(aluno.getCidade());
        dto.setEstado(aluno.getEstado());
        dto.setCep(aluno.getCep());
        dto.setBairro(aluno.getBairro());
        dto.setNumero(aluno.getNumero());
        dto.setComplemento(aluno.getComplemento());
        dto.setDataNascimento(aluno.getDataNascimento());
        dto.setIdade(aluno.getIdade());
        dto.setNaturalidade(aluno.getNaturalidade());
        dto.setNomePai(aluno.getNomePai());
        dto.setNomeMae(aluno.getNomeMae());
        dto.setDataCadastro(aluno.getDataCadastro());
        dto.setStatus(aluno.getStatus().toString());
        
        // Calcular média geral de todas as matrículas
        if (aluno.getMatriculas() != null && !aluno.getMatriculas().isEmpty()) {
            double mediaTotal = aluno.getMatriculas().stream()
                    .mapToDouble(m -> m.getMediaFinal() != null ? m.getMediaFinal() : 0.0)
                    .average()
                    .orElse(0.0);
  // No toDTO(), remover:
// dto.setMediaGeral(mediaTotal);
            dto.setTotalMatriculas(aluno.getMatriculas().size());
            
            // Buscar curso da última matrícula ativa
            aluno.getMatriculas().stream()
                .filter(m -> m.getStatus() == MatriculaModel.StatusMatricula.CURSANDO)
                .findFirst()
                .ifPresent(m -> {
                    CursoDTO cursoDTO = new CursoDTO();
                    cursoDTO.setId(m.getCurso().getId());
                    cursoDTO.setNome(m.getCurso().getNome());
                    dto.setCursoAtual(cursoDTO);
                });
        } else {
// No toDTO(), remover:
// dto.setMediaGeral(mediaTotal);
            dto.setTotalMatriculas(0);
        }
        
        return dto;
    }

    private String gerarMatricula() {
        String ano = String.valueOf(LocalDate.now().getYear());
        long count = alunoRepository.count() + 1;
        String sequencial = String.format("%05d", count);
        return "IBA" + ano + sequencial;
    }

    public List<AlunoDTO> findAll() {
        return alunoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AlunoDTO findById(Long id) {
        AlunoModel aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
        return toDTO(aluno);
    }

    // REMOVER este método - não existe mais findByCursoId em Aluno
    // public List<AlunoDTO> findByCursoId(Long cursoId) {
    //     return alunoRepository.findByCursoId(cursoId).stream()
    //             .map(this::toDTO)
    //             .collect(Collectors.toList());
    // }

    public List<AlunoDTO> searchByNome(String nome) {
        return alunoRepository.searchByNome(nome).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlunoDTO create(AlunoDTO alunoDTO) {
        // Validações
        if (alunoDTO.getCpf() != null && !alunoDTO.getCpf().isEmpty() && alunoRepository.existsByCpf(alunoDTO.getCpf())) {
            throw new RuntimeException("CPF já cadastrado");
        }

        AlunoModel aluno = new AlunoModel();
        aluno.setMatricula(gerarMatricula());
        aluno.setNome(alunoDTO.getNome());
        aluno.setCpf(alunoDTO.getCpf());
        aluno.setRg(alunoDTO.getRg());
        aluno.setEmail(alunoDTO.getEmail());
        aluno.setTelefone(alunoDTO.getTelefone());
        aluno.setEndereco(alunoDTO.getEndereco());
        aluno.setCidade(alunoDTO.getCidade());
        aluno.setEstado(alunoDTO.getEstado());
        aluno.setCep(alunoDTO.getCep());
        aluno.setBairro(alunoDTO.getBairro());
        aluno.setNumero(alunoDTO.getNumero());
        aluno.setComplemento(alunoDTO.getComplemento());
        aluno.setDataNascimento(alunoDTO.getDataNascimento());
        aluno.setNaturalidade(alunoDTO.getNaturalidade());
        aluno.setNomePai(alunoDTO.getNomePai());
        aluno.setNomeMae(alunoDTO.getNomeMae());
        aluno.setStatus(AlunoModel.StatusAluno.ATIVO);

        AlunoModel saved = alunoRepository.save(aluno);
        return toDTO(saved);
    }

    @Transactional
    public AlunoDTO update(Long id, AlunoDTO alunoDTO) {
        AlunoModel aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));

        if (alunoDTO.getNome() != null) aluno.setNome(alunoDTO.getNome());
        if (alunoDTO.getCpf() != null) aluno.setCpf(alunoDTO.getCpf());
        if (alunoDTO.getRg() != null) aluno.setRg(alunoDTO.getRg());
        if (alunoDTO.getEmail() != null) aluno.setEmail(alunoDTO.getEmail());
        if (alunoDTO.getTelefone() != null) aluno.setTelefone(alunoDTO.getTelefone());
        if (alunoDTO.getEndereco() != null) aluno.setEndereco(alunoDTO.getEndereco());
        if (alunoDTO.getCidade() != null) aluno.setCidade(alunoDTO.getCidade());
        if (alunoDTO.getEstado() != null) aluno.setEstado(alunoDTO.getEstado());
        if (alunoDTO.getCep() != null) aluno.setCep(alunoDTO.getCep());
        if (alunoDTO.getBairro() != null) aluno.setBairro(alunoDTO.getBairro());
        if (alunoDTO.getNumero() != null) aluno.setNumero(alunoDTO.getNumero());
        if (alunoDTO.getComplemento() != null) aluno.setComplemento(alunoDTO.getComplemento());
        if (alunoDTO.getDataNascimento() != null) aluno.setDataNascimento(alunoDTO.getDataNascimento());
        if (alunoDTO.getNaturalidade() != null) aluno.setNaturalidade(alunoDTO.getNaturalidade());
        if (alunoDTO.getNomePai() != null) aluno.setNomePai(alunoDTO.getNomePai());
        if (alunoDTO.getNomeMae() != null) aluno.setNomeMae(alunoDTO.getNomeMae());

        AlunoModel saved = alunoRepository.save(aluno);
        return toDTO(saved);
    }

    @Transactional
    public AlunoDTO updateStatus(Long id, String status) {
        AlunoModel aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
        
        aluno.setStatus(AlunoModel.StatusAluno.valueOf(status));
        AlunoModel saved = alunoRepository.save(aluno);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        AlunoModel aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
        
        // Histórico concluído também é documento acadêmico. Desligue o aluno
        // em vez de apagar matrículas e notas por cascata.
        if (aluno.getMatriculas() != null && !aluno.getMatriculas().isEmpty()) {
            throw new IllegalArgumentException("Aluno possui histórico acadêmico. Altere o status em vez de excluir.");
        }
        
        alunoRepository.deleteById(id);
    }
}