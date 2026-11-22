(function () {
"use strict";
// Doacoes Page Script

async function carregarDoacoes() {
    try {
        const doacoes = await apiRequest('/doacoes');
        const tbody = document.getElementById('doacoesList');
        tbody.innerHTML = '';
        
        if (doacoes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center">Nenhuma doação registrada</td></tr>';
            return;
        }
        
        doacoes.forEach(doacao => {
            const valorItem = doacao.tipo === 'DINHEIRO' 
                ? `R$ ${doacao.valor?.toFixed(2)}` 
                : doacao.descricaoItem || '-';
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${escapeHtml(doacao.nomeDoador)}</strong><br><small>${escapeHtml(doacao.email || '')}</small></td>
                <td>${escapeHtml(doacao.tipoDescricao)}</td>
                <td>${escapeHtml(valorItem)}</td>
                <td>${escapeHtml(doacao.dataFormatada)}</td>
                <td><span class="status-badge ${getStatusClass(doacao.status)}">${escapeHtml(doacao.statusDescricao)}</span></td>
                <td class="actions">
                    <button class="btn btn-sm btn-outline" onclick="verDoacao(${doacao.id})" title="Visualizar">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-outline" onclick="editarDoacao(${doacao.id})" title="Editar">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="removerDoacao(${doacao.id})" title="Remover">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (error) {
        showAlert('Erro ao carregar doações: ' + error.message, 'error');
    }
}

async function carregarDashboardDoacoes() {
    try {
        const dashboard = await apiRequest('/doacoes/dashboard');
        
        document.getElementById('totalDoacoes').innerText = dashboard.totalDoacoes || 0;
        document.getElementById('totalConfirmadas').innerText = dashboard.totalConfirmadas || 0;
        document.getElementById('totalDinheiro').innerHTML = `R$ ${(dashboard.totalDinheiro || 0).toFixed(2)}`;
        
        // Distribuição por tipo
        const tbody = document.getElementById('distribuicaoTipos');
        tbody.innerHTML = '';
        
        const total = dashboard.totalConfirmadas || 1;
        for (const [tipo, quantidade] of Object.entries(dashboard.doacoesPorTipo || {})) {
            const percentual = (quantidade / total * 100).toFixed(1);
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(tipo)}</td>
                <td>${quantidade}</td>
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <div style="flex:1; background: #e0e0e0; border-radius: 10px; height: 8px;">
                            <div style="width: ${percentual}%; background: var(--primary); border-radius: 10px; height: 8px;"></div>
                        </div>
                        <span>${percentual}%</span>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        }
    } catch (error) {
        console.error('Erro ao carregar dashboard:', error);
    }
}

function toggleValorItem() {
    const tipo = document.getElementById('tipoDoacao').value;
    const valorGroup = document.getElementById('valorGroup');
    const descricaoGroup = document.getElementById('descricaoGroup');
    
    if (tipo === 'DINHEIRO') {
        valorGroup.style.display = 'flex';
        descricaoGroup.style.display = 'none';
        document.getElementById('descricaoItem').required = false;
        document.getElementById('valor').required = true;
    } else if (tipo && tipo !== '') {
        valorGroup.style.display = 'none';
        descricaoGroup.style.display = 'flex';
        document.getElementById('valor').required = false;
        document.getElementById('descricaoItem').required = true;
    } else {
        valorGroup.style.display = 'none';
        descricaoGroup.style.display = 'none';
    }
}

async function salvarDoacao(event) {
    event.preventDefault();
    
    const id = document.getElementById('editDoacaoId').value;
    const tipo = document.getElementById('tipoDoacao').value;
    
    const doacaoData = {
        nomeDoador: document.getElementById('nomeDoador').value,
        email: document.getElementById('email').value,
        telefone: document.getElementById('telefone').value,
        tipo: tipo,
        valor: tipo === 'DINHEIRO' ? parseFloat(document.getElementById('valor').value) : null,
        descricaoItem: tipo !== 'DINHEIRO' ? document.getElementById('descricaoItem').value : null,
        dataDoacao: document.getElementById('dataDoacao').value || null,
        status: document.getElementById('statusDoacao').value,
        observacoes: document.getElementById('observacoes').value
    };
    
    if (!doacaoData.nomeDoador || !tipo) {
        showAlert('Preencha nome do doador e tipo de doação!', 'error');
        return;
    }
    
    if (tipo === 'DINHEIRO' && (!doacaoData.valor || doacaoData.valor <= 0)) {
        showAlert('Informe um valor válido para a doação!', 'error');
        return;
    }
    
    if (tipo !== 'DINHEIRO' && !doacaoData.descricaoItem) {
        showAlert('Descreva o item doado!', 'error');
        return;
    }
    
    try {
        if (id) {
            await apiRequest(`/doacoes/${id}`, 'PUT', doacaoData);
            showAlert('Doação atualizada com sucesso!', 'success');
        } else {
            await apiRequest('/doacoes', 'POST', doacaoData);
            showAlert('Doação registrada com sucesso!', 'success');
        }
        limparFormDoacao();
        carregarDoacoes();
        carregarDashboardDoacoes();
    } catch (error) {
        showAlert('Erro ao salvar: ' + error.message, 'error');
    }
}

async function verDoacao(id) {
    try {
        const doacao = await apiRequest(`/doacoes/${id}`);
        const modalBody = document.getElementById('doacaoDetalhes');
        
        modalBody.innerHTML = `
            <div class="form-grid">
                <div><strong>Doador:</strong><br>${escapeHtml(doacao.nomeDoador)}</div>
                <div><strong>Email:</strong><br>${escapeHtml(doacao.email || '-')}</div>
                <div><strong>Telefone:</strong><br>${escapeHtml(doacao.telefone || '-')}</div>
                <div><strong>Tipo:</strong><br>${escapeHtml(doacao.tipoDescricao)}</div>
                <div><strong>${doacao.tipo === 'DINHEIRO' ? 'Valor:' : 'Item:'}</strong><br>${doacao.tipo === 'DINHEIRO' ? `R$ ${doacao.valor?.toFixed(2)}` : escapeHtml(doacao.descricaoItem || '-')}</div>
                <div><strong>Data:</strong><br>${escapeHtml(doacao.dataFormatada)}</div>
                <div><strong>Status:</strong><br><span class="status-badge ${getStatusClass(doacao.status)}">${escapeHtml(doacao.statusDescricao)}</span></div>
                <div><strong>Observações:</strong><br>${escapeHtml(doacao.observacoes || '-')}</div>
            </div>
        `;
        
        document.getElementById('doacaoModal').classList.add('active');
        window.currentDoacaoId = id;
    } catch (error) {
        showAlert('Erro ao carregar detalhes: ' + error.message, 'error');
    }
}

async function editarDoacao(id) {
    try {
        const doacao = await apiRequest(`/doacoes/${id}`);
        document.getElementById('editDoacaoId').value = doacao.id;
        document.getElementById('nomeDoador').value = doacao.nomeDoador;
        document.getElementById('email').value = doacao.email || '';
        document.getElementById('telefone').value = doacao.telefone || '';
        document.getElementById('tipoDoacao').value = doacao.tipo;
        document.getElementById('statusDoacao').value = doacao.status;
        document.getElementById('observacoes').value = doacao.observacoes || '';
        document.getElementById('dataDoacao').value = doacao.dataDoacao || '';
        
        toggleValorItem();
        
        if (doacao.tipo === 'DINHEIRO') {
            document.getElementById('valor').value = doacao.valor;
        } else {
            document.getElementById('descricaoItem').value = doacao.descricaoItem || '';
        }
        
        document.getElementById('doacaoForm').scrollIntoView({ behavior: 'smooth' });
        if (document.getElementById('doacaoModal').classList.contains('active')) {
            fecharModal('doacaoModal');
        }
    } catch (error) {
        showAlert('Erro ao carregar doação: ' + error.message, 'error');
    }
}

function editarDoacaoFromModal() {
    if (window.currentDoacaoId) {
        editarDoacao(window.currentDoacaoId);
    }
}

async function removerDoacao(id) {
    if (confirm('⚠️ Tem certeza que deseja remover esta doação? Esta ação não pode ser desfeita.')) {
        try {
            await apiRequest(`/doacoes/${id}`, 'DELETE');
            showAlert('Doação removida com sucesso!', 'success');
            carregarDoacoes();
            carregarDashboardDoacoes();
        } catch (error) {
            showAlert('Erro ao remover: ' + error.message, 'error');
        }
    }
}

function limparFormDoacao() {
    document.getElementById('editDoacaoId').value = '';
    document.getElementById('doacaoForm').reset();
    document.getElementById('valorGroup').style.display = 'none';
    document.getElementById('descricaoGroup').style.display = 'none';
}

function fecharModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}


// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { carregarDoacoes, carregarDashboardDoacoes, toggleValorItem, salvarDoacao, verDoacao, editarDoacao, editarDoacaoFromModal, removerDoacao, limparFormDoacao, fecharModal });

// Event Listeners
document.getElementById('doacaoForm').addEventListener('submit', salvarDoacao);
document.getElementById('tipoDoacao').addEventListener('change', toggleValorItem);
document.getElementById('telefone').addEventListener('input', (e) => { e.target.value = formatTelefone(e.target.value); });

// Adicionar ao menu no index.html (você precisará adicionar manualmente)
// Inicializar
carregarDoacoes();
carregarDashboardDoacoes();
})();
