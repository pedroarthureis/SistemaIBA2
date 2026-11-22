(function () {
"use strict";
// Professores Page Script
let todosProfessores = [];

async function carregarProfessores() {
    try {
        const professores = await ProfessorAPI.getAll();
        todosProfessores = professores;
        renderizarProfessores(professores);
    } catch (error) {
        showAlert('Erro ao carregar professores: ' + error.message, 'error');
    }
}

function renderizarProfessores(professores) {
    const tbody = document.getElementById('professoresList');
    tbody.innerHTML = '';
    
    if (professores.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center">Nenhum professor cadastrado</td></tr>';
        return;
    }
    
    professores.forEach(prof => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${prof.id}</td>
            <td><strong>${escapeHtml(prof.nome)}</strong></td>
            <td>${escapeHtml(formatCPF(prof.cpf))}</td>
            <td>${escapeHtml(prof.area || 'Não informada')}</td>
            <td><span class="status-badge ${getStatusClass(prof.status)}">${escapeHtml(prof.status || 'ATIVO')}</span></td>
            <td class="actions">
                <button class="btn btn-sm btn-outline" onclick="verProfessor(${prof.id})" title="Visualizar">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-sm btn-outline" onclick="editarProfessor(${prof.id})" title="Editar">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="removerProfessor(${prof.id})" title="Remover">
                    <i class="fas fa-trash"></i>
                </button>
             </td>
        `;
        tbody.appendChild(tr);
    });
}

function buscarProfessores() {
    const termo = document.getElementById('searchProfessor').value.toLowerCase();
    if (!termo) {
        renderizarProfessores(todosProfessores);
        return;
    }
    
    const filtrados = todosProfessores.filter(p => 
        p.nome.toLowerCase().includes(termo) || 
        (p.area && p.area.toLowerCase().includes(termo)) ||
        (p.cpf && p.cpf.includes(termo))
    );
    renderizarProfessores(filtrados);
}

async function salvarProfessor(event) {
    event.preventDefault();
    
    const id = document.getElementById('editProfessorId').value;
    let cpf = document.getElementById('cpf').value;
    cpf = cpf.replace(/\D/g, '');
    
    if (!document.getElementById('nome').value || !cpf) {
        showAlert('Preencha nome e CPF!', 'error');
        return;
    }
    
    if (cpf.length !== 11) {
        showAlert('CPF inválido! Digite 11 números.', 'error');
        return;
    }
    
    const professorData = {
        nome: document.getElementById('nome').value,
        cpf: cpf,
        rg: document.getElementById('rg').value,
        email: document.getElementById('email').value,
        telefone: document.getElementById('telefone').value,
        endereco: document.getElementById('endereco').value,
        numero: document.getElementById('numero').value,
        complemento: document.getElementById('complemento').value,
        bairro: document.getElementById('bairro').value,
        cidade: document.getElementById('cidade').value,
        estado: document.getElementById('estado').value,
        cep: document.getElementById('cep').value?.replace(/\D/g, ''),
        dataNascimento: document.getElementById('dataNascimento').value,
        area: document.getElementById('area').value,
        especializacao: document.getElementById('especializacao').value,
        dataContratacao: document.getElementById('dataContratacao').value
    };
    
    try {
        if (id) {
            await ProfessorAPI.update(id, professorData);
            showAlert('Professor atualizado com sucesso!');
        } else {
            await ProfessorAPI.create(professorData);
            showAlert('Professor cadastrado com sucesso!');
        }
        limparFormProfessor();
        carregarProfessores();
    } catch (error) {
        showAlert('Erro ao salvar: ' + error.message, 'error');
    }
}

async function editarProfessor(id) {
    try {
        const prof = await ProfessorAPI.getById(id);
        document.getElementById('editProfessorId').value = prof.id;
        document.getElementById('nome').value = prof.nome || '';
        document.getElementById('cpf').value = prof.cpf || '';
        document.getElementById('rg').value = prof.rg || '';
        document.getElementById('email').value = prof.email || '';
        document.getElementById('telefone').value = prof.telefone || '';
        document.getElementById('endereco').value = prof.endereco || '';
        document.getElementById('numero').value = prof.numero || '';
        document.getElementById('complemento').value = prof.complemento || '';
        document.getElementById('bairro').value = prof.bairro || '';
        document.getElementById('cidade').value = prof.cidade || '';
        document.getElementById('estado').value = prof.estado || '';
        document.getElementById('cep').value = prof.cep || '';
        document.getElementById('dataNascimento').value = formatDateInput(prof.dataNascimento);
        document.getElementById('area').value = prof.area || '';
        document.getElementById('especializacao').value = prof.especializacao || '';
        document.getElementById('dataContratacao').value = formatDateInput(prof.dataContratacao);
        
        document.getElementById('professorForm').scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showAlert('Erro ao carregar professor: ' + error.message, 'error');
    }
}

async function verProfessor(id) {
    try {
        const prof = await ProfessorAPI.getById(id);
        const modalBody = document.getElementById('professorDetalhes');
        modalBody.innerHTML = `
            <div class="form-grid">
                <div><strong>ID:</strong><br>${prof.id}</div>
                <div><strong>Nome:</strong><br>${escapeHtml(prof.nome)}</div>
                <div><strong>CPF:</strong><br>${escapeHtml(formatCPF(prof.cpf))}</div>
                <div><strong>RG:</strong><br>${escapeHtml(prof.rg || '-')}</div>
                <div><strong>Email:</strong><br>${escapeHtml(prof.email || '-')}</div>
                <div><strong>Telefone:</strong><br>${escapeHtml(prof.telefone || '-')}</div>
                <div><strong>Data Nascimento:</strong><br>${formatDate(prof.dataNascimento)}</div>
                <div><strong>Idade:</strong><br>${prof.idade || '-'} anos</div>
                <div><strong>Endereço:</strong><br>${escapeHtml(prof.endereco || '-')}, ${escapeHtml(prof.numero || '')} - ${escapeHtml(prof.bairro || '')}</div>
                <div><strong>Cidade/UF:</strong><br>${escapeHtml(prof.cidade || '')}/${escapeHtml(prof.estado || '')}</div>
                <div><strong>CEP:</strong><br>${escapeHtml(formatCEP(prof.cep || ''))}</div>
                <div><strong>Área:</strong><br>${escapeHtml(prof.area || '-')}</div>
                <div><strong>Especialização:</strong><br>${escapeHtml(prof.especializacao || '-')}</div>
                <div><strong>Data Contratação:</strong><br>${formatDate(prof.dataContratacao)}</div>
                <div><strong>Status:</strong><br><span class="status-badge ${getStatusClass(prof.status)}">${escapeHtml(prof.status || 'ATIVO')}</span></div>
            </div>
        `;
        document.getElementById('professorModal').classList.add('active');
    } catch (error) {
        showAlert('Erro ao carregar detalhes: ' + error.message, 'error');
    }
}

async function removerProfessor(id, nome = "este registro") {
    if (confirm(`⚠️ Remover o professor "${nome}"?`)) {
        try {
            await ProfessorAPI.delete(id);
            showAlert('Professor removido com sucesso!');
            carregarProfessores();
        } catch (error) {
            showAlert('Erro ao remover: ' + error.message, 'error');
        }
    }
}

async function buscarCepProfessor() {
    const cepInput = document.getElementById('cep');
    await buscarEnderecoPorCep(cepInput, {
        logradouro: 'endereco',
        bairro: 'bairro',
        cidade: 'cidade',
        estado: 'estado',
        complemento: 'complemento'
    });
}

function limparFormProfessor() {
    document.getElementById('editProfessorId').value = '';
    document.getElementById('professorForm').reset();
}

function fecharModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}


// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { carregarProfessores, buscarProfessores, salvarProfessor, editarProfessor, verProfessor, removerProfessor, buscarCepProfessor, limparFormProfessor, fecharModal });

// Event Listeners
document.getElementById('professorForm').addEventListener('submit', salvarProfessor);
document.getElementById('cpf').addEventListener('input', (e) => { e.target.value = formatCPF(e.target.value); });
document.getElementById('telefone').addEventListener('input', (e) => { e.target.value = formatTelefone(e.target.value); });
document.getElementById('cep').addEventListener('input', (e) => { e.target.value = formatCEP(e.target.value); });

// Inicializar
carregarProfessores();
})();
