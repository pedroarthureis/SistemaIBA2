package com.iba.service;

import com.iba.audit.AuditEvents;

import com.iba.dto.DoacaoDTO;
import com.iba.dto.DashboardDoacoesDTO;  // ← ADICIONAR ESTE IMPORT
import com.iba.model.DoacaoModel;
import com.iba.repository.DoacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoacaoService {

    private final DoacaoRepository doacaoRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DoacaoDTO toDTO(DoacaoModel doacao) {
        DoacaoDTO dto = new DoacaoDTO();
        dto.setId(doacao.getId());
        dto.setNomeDoador(doacao.getNomeDoador());
        dto.setEmail(doacao.getEmail());
        dto.setTelefone(doacao.getTelefone());
        dto.setTipo(doacao.getTipo().name());
        dto.setTipoDescricao(doacao.getTipo().getDescricao());
        dto.setValor(doacao.getValor());
        dto.setDescricaoItem(doacao.getDescricaoItem());
        dto.setDataDoacao(doacao.getDataDoacao());
        dto.setStatus(doacao.getStatus().name());
        dto.setStatusDescricao(doacao.getStatus().getDescricao());
        dto.setObservacoes(doacao.getObservacoes());
        dto.setDataFormatada(doacao.getDataDoacao().format(DATE_FORMATTER));
        
        if (doacao.getValor() != null) {
            dto.setValorFormatado(String.format("R$ %.2f", doacao.getValor()));
        }
        
        return dto;
    }

    public List<DoacaoDTO> findAll() {
        return doacaoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DoacaoDTO findById(Long id) {
        DoacaoModel doacao = doacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doação não encontrada"));
        return toDTO(doacao);
    }

    public List<DoacaoDTO> findByStatus(String status) {
        return doacaoRepository.findByStatus(DoacaoModel.StatusDoacao.valueOf(status)).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<DoacaoDTO> findByPeriodo(LocalDate inicio, LocalDate fim) {
        return doacaoRepository.findByDataDoacaoBetween(inicio, fim).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DoacaoDTO create(DoacaoDTO doacaoDTO) {
        DoacaoModel doacao = new DoacaoModel();
        doacao.setNomeDoador(doacaoDTO.getNomeDoador());
        doacao.setEmail(doacaoDTO.getEmail());
        doacao.setTelefone(doacaoDTO.getTelefone());
        doacao.setTipo(DoacaoModel.TipoDoacao.valueOf(doacaoDTO.getTipo()));
        doacao.setValor(doacaoDTO.getValor());
        doacao.setDescricaoItem(doacaoDTO.getDescricaoItem());
        doacao.setDataDoacao(doacaoDTO.getDataDoacao() != null ? doacaoDTO.getDataDoacao() : LocalDate.now());
        doacao.setStatus(DoacaoModel.StatusDoacao.valueOf(doacaoDTO.getStatus() != null ? doacaoDTO.getStatus() : "CONFIRMADA"));
        doacao.setObservacoes(doacaoDTO.getObservacoes());

        validarDoacao(doacao);
        DoacaoModel saved = doacaoRepository.save(doacao);
        AuditEvents.afterCommit("CREATE", "doacao", saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public DoacaoDTO update(Long id, DoacaoDTO doacaoDTO) {
        DoacaoModel doacao = doacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doação não encontrada"));

        if (doacaoDTO.getNomeDoador() != null) doacao.setNomeDoador(doacaoDTO.getNomeDoador());
        if (doacaoDTO.getEmail() != null) doacao.setEmail(doacaoDTO.getEmail());
        if (doacaoDTO.getTelefone() != null) doacao.setTelefone(doacaoDTO.getTelefone());
        if (doacaoDTO.getTipo() != null) doacao.setTipo(DoacaoModel.TipoDoacao.valueOf(doacaoDTO.getTipo()));
        if (doacaoDTO.getValor() != null) doacao.setValor(doacaoDTO.getValor());
        if (doacaoDTO.getDescricaoItem() != null) doacao.setDescricaoItem(doacaoDTO.getDescricaoItem());
        if (doacaoDTO.getDataDoacao() != null) doacao.setDataDoacao(doacaoDTO.getDataDoacao());
        if (doacaoDTO.getStatus() != null) doacao.setStatus(DoacaoModel.StatusDoacao.valueOf(doacaoDTO.getStatus()));
        if (doacaoDTO.getObservacoes() != null) doacao.setObservacoes(doacaoDTO.getObservacoes());

        validarDoacao(doacao);
        DoacaoModel saved = doacaoRepository.save(doacao);
        AuditEvents.afterCommit("UPDATE", "doacao", saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!doacaoRepository.existsById(id)) {
            throw new RuntimeException("Doação não encontrada");
        }
        doacaoRepository.deleteById(id);
        AuditEvents.afterCommit("DELETE", "doacao", id);
    }

    private void validarDoacao(DoacaoModel doacao) {
        if (doacao.getNomeDoador() == null || doacao.getNomeDoador().isBlank()) {
            throw new IllegalArgumentException("Nome do doador é obrigatório");
        }
        if (doacao.getTipo() == null) throw new IllegalArgumentException("Tipo de doação é obrigatório");
        if (doacao.getTipo() == DoacaoModel.TipoDoacao.DINHEIRO) {
            if (doacao.getValor() == null || !Double.isFinite(doacao.getValor()) || doacao.getValor() <= 0) {
                throw new IllegalArgumentException("Valor da doação deve ser maior que zero");
            }
        } else if (doacao.getDescricaoItem() == null || doacao.getDescricaoItem().isBlank()) {
            throw new IllegalArgumentException("Descreva o item doado");
        }
    }

    public DashboardDoacoesDTO getDashboardData() {
        DashboardDoacoesDTO dashboard = new DashboardDoacoesDTO();
        
        dashboard.setTotalDoacoes(doacaoRepository.count());
        dashboard.setTotalConfirmadas(doacaoRepository.countDoacoesConfirmadas());
        dashboard.setTotalDinheiro(doacaoRepository.sumValorDoacoesDinheiro() != null ? doacaoRepository.sumValorDoacoesDinheiro() : 0.0);
        
        List<Object[]> doacoesPorTipo = doacaoRepository.countByTipo();
        for (Object[] item : doacoesPorTipo) {
            DoacaoModel.TipoDoacao tipo = (DoacaoModel.TipoDoacao) item[0];
            Long quantidade = (Long) item[1];
            dashboard.getDoacoesPorTipo().put(tipo.getDescricao(), quantidade);
        }
        
        // Últimas 5 doações
        dashboard.setUltimasDoacoes(doacaoRepository.findAll().stream()
                .sorted((a, b) -> b.getDataDoacao().compareTo(a.getDataDoacao()))
                .limit(5)
                .map(this::toDTO)
                .collect(Collectors.toList()));
        
        return dashboard;
    }
}