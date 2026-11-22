(function () {
"use strict";
// Alunos Page Script

async function carregarAlunos() {
    try {
        const alunos = await AlunoAPI.getAll();
        const tbody = document.getElementById('alunosList');
        tbody.innerHTML = '';
        
        if (alunos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center">Nenhum aluno cadastrado</td></tr>';
            return;
        }
        
        for (const aluno of alunos) {
            const cursoAtual = aluno.cursoAtual?.nome || 'Não matriculado';
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${escapeHtml(aluno.matricula || aluno.id)}</strong></td>
                <td>${escapeHtml(aluno.nome)}</td>
                <td>${escapeHtml(formatCPF(aluno.cpf || ''))}</td>
                <td>${escapeHtml(cursoAtual)}</td>
                <td><span class="status-badge ${getStatusClass(aluno.status)}">${escapeHtml(aluno.status || 'ATIVO')}</span></td>
                <td class="actions">
                    <button class="btn btn-sm btn-outline" onclick="verAluno(${aluno.id})" title="Visualizar">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-outline" onclick="editarAluno(${aluno.id})" title="Editar">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="removerAluno(${aluno.id})" title="Remover">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        }
    } catch (error) {
        showAlert('Erro ao carregar alunos: ' + error.message, 'error');
    }
}

async function buscarAlunos() {
    const termo = document.getElementById('searchAluno').value;
    if (!termo) {
        carregarAlunos();
        return;
    }
    
    try {
        const alunos = await AlunoAPI.search(termo);
        const tbody = document.getElementById('alunosList');
        tbody.innerHTML = '';
        
        for (const aluno of alunos) {
            const cursoAtual = aluno.cursoAtual?.nome || 'Não matriculado';
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(aluno.matricula || aluno.id)}</td>
                <td>${escapeHtml(aluno.nome)}</td>
                <td>${escapeHtml(formatCPF(aluno.cpf || ''))}</td>
                <td>${escapeHtml(cursoAtual)}</td>
                <td><span class="status-badge ${getStatusClass(aluno.status)}">${escapeHtml(aluno.status || 'ATIVO')}</span></td>
                <td class="actions">
                    <button class="btn btn-sm btn-outline" onclick="verAluno(${aluno.id})"><i class="fas fa-eye"></i></button>
                    <button class="btn btn-sm btn-outline" onclick="editarAluno(${aluno.id})"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-sm btn-danger" onclick="removerAluno(${aluno.id})"><i class="fas fa-trash"></i></button>
                </td>
            `;
            tbody.appendChild(tr);
        }
    } catch (error) {
        showAlert('Erro na busca: ' + error.message, 'error');
    }
}

async function salvarAluno(event) {
    event.preventDefault();
    
    const id = document.getElementById('editAlunoId').value;
    const alunoData = {
        nome: document.getElementById('nome').value,
        cpf: document.getElementById('cpf').value?.replace(/\D/g, ''),
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
        naturalidade: document.getElementById('naturalidade').value,
        nomePai: document.getElementById('nomePai').value,
        nomeMae: document.getElementById('nomeMae').value
    };
    
    if (!alunoData.nome) {
        showAlert('Preencha o nome do aluno!', 'error');
        return;
    }
    
    try {
        if (id) {
            await AlunoAPI.update(id, alunoData);
            showAlert('Aluno atualizado com sucesso!');
        } else {
            await AlunoAPI.create(alunoData);
            showAlert('Aluno cadastrado com sucesso!');
        }
        limparFormAluno();
        carregarAlunos();
    } catch (error) {
        showAlert('Erro ao salvar: ' + error.message, 'error');
    }
}

async function editarAluno(id) {
    try {
        const aluno = await AlunoAPI.getById(id);
        document.getElementById('editAlunoId').value = aluno.id;
        document.getElementById('nome').value = aluno.nome || '';
        document.getElementById('cpf').value = aluno.cpf || '';
        document.getElementById('rg').value = aluno.rg || '';
        document.getElementById('email').value = aluno.email || '';
        document.getElementById('telefone').value = aluno.telefone || '';
        document.getElementById('endereco').value = aluno.endereco || '';
        document.getElementById('numero').value = aluno.numero || '';
        document.getElementById('complemento').value = aluno.complemento || '';
        document.getElementById('bairro').value = aluno.bairro || '';
        document.getElementById('cidade').value = aluno.cidade || '';
        document.getElementById('estado').value = aluno.estado || '';
        document.getElementById('cep').value = aluno.cep || '';
        document.getElementById('dataNascimento').value = formatDateInput(aluno.dataNascimento);
        document.getElementById('naturalidade').value = aluno.naturalidade || '';
        document.getElementById('nomePai').value = aluno.nomePai || '';
        document.getElementById('nomeMae').value = aluno.nomeMae || '';
        
        document.getElementById('alunoForm').scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showAlert('Erro ao carregar aluno: ' + error.message, 'error');
    }
}

async function verAluno(id) {
    try {
        const aluno = await AlunoAPI.getById(id);
        const matriculas = await MatriculaAPI.getByAluno(id);
        
        const modalBody = document.getElementById('alunoDetalhes');
        modalBody.innerHTML = `
            <div class="form-grid">
                <div><strong>Matrícula:</strong><br>${escapeHtml(aluno.matricula || aluno.id)}</div>
                <div><strong>Nome:</strong><br>${escapeHtml(aluno.nome)}</div>
                <div><strong>CPF:</strong><br>${escapeHtml(formatCPF(aluno.cpf || ''))}</div>
                <div><strong>RG:</strong><br>${escapeHtml(aluno.rg || '-')}</div>
                <div><strong>Email:</strong><br>${escapeHtml(aluno.email || '-')}</div>
                <div><strong>Telefone:</strong><br>${escapeHtml(aluno.telefone || '-')}</div>
                <div><strong>Data Nascimento:</strong><br>${formatDate(aluno.dataNascimento)}</div>
                <div><strong>Idade:</strong><br>${aluno.idade || '-'} anos</div>
                <div><strong>Naturalidade:</strong><br>${escapeHtml(aluno.naturalidade || '-')}</div>
                <div><strong>Pai:</strong><br>${escapeHtml(aluno.nomePai || '-')}</div>
                <div><strong>Mãe:</strong><br>${escapeHtml(aluno.nomeMae || '-')}</div>
                <div><strong>Endereço:</strong><br>${escapeHtml(aluno.endereco || '-')}, ${escapeHtml(aluno.numero || '')} - ${escapeHtml(aluno.bairro || '')}</div>
                <div><strong>Cidade/UF:</strong><br>${escapeHtml(aluno.cidade || '')}/${escapeHtml(aluno.estado || '')}</div>
                <div><strong>CEP:</strong><br>${escapeHtml(formatCEP(aluno.cep || ''))}</div>
                <div><strong>Data Cadastro:</strong><br>${formatDate(aluno.dataCadastro)}</div>
                <div><strong>Status:</strong><br><span class="status-badge ${getStatusClass(aluno.status)}">${escapeHtml(aluno.status || 'ATIVO')}</span></div>
            </div>
            <hr style="margin: 15px 0">
            <h4>Histórico de Matrículas</h4>
            <div class="table-container">
                <table class="data-table" style="margin-top: 10px">
                    <thead>
                        <tr><th>Curso</th><th>Ano</th><th>Semestre</th><th>Status</th><th>Ações</th></tr>
                    </thead>
                    <tbody>
                        ${matriculas.map(m => `
                            <tr>
                                <td>${escapeHtml(m.cursoNome)}</td>
                                <td>${m.ano}</td>
                                <td>${m.semestre}º</td>
                                <td><span class="status-badge ${getStatusClass(m.status)}">${escapeHtml(m.status)}</span></td>
                                <td>
                                    <button class="btn btn-sm btn-outline" onclick="fecharModal('alunoModal'); setTimeout(() => carregarNotasMatricula(${m.id}), 100)">
                                        <i class="fas fa-star"></i> Ver Notas
                                    </button>
                                </td>
                            </tr>
                        `).join('') || '<tr><td colspan="5">Nenhuma matrícula encontrada</td></tr>'}
                    </tbody>
                </table>
            </div>
        `;
        
        document.getElementById('alunoModal').classList.add('active');
    } catch (error) {
        showAlert('Erro ao carregar detalhes: ' + error.message, 'error');
    }
}

async function removerAluno(id) {
    if (confirm('⚠️ Tem certeza que deseja remover este aluno? Alunos com histórico acadêmico não poderão ser excluídos. Prefira alterar o status.')) {
        try {
            await AlunoAPI.delete(id);
            showAlert('Aluno removido com sucesso!');
            carregarAlunos();
        } catch (error) {
            showAlert('Erro ao remover: ' + error.message, 'error');
        }
    }
}

async function buscarCepAluno() {
    const cepInput = document.getElementById('cep');
    await buscarEnderecoPorCep(cepInput, {
        logradouro: 'endereco',
        bairro: 'bairro',
        cidade: 'cidade',
        estado: 'estado',
        complemento: 'complemento'
    });
}

function limparFormAluno() {
    document.getElementById('editAlunoId').value = '';
    document.getElementById('alunoForm').reset();
}

function fecharModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

function carregarNotasMatricula(matriculaId) {
    const notasMenuItem = document.querySelector('.menu-item[data-page="notas"]');
    if (notasMenuItem) {
        notasMenuItem.click();
        sessionStorage.setItem('matriculaSelecionadaId', matriculaId);
    }
}


// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { carregarAlunos, buscarAlunos, salvarAluno, editarAluno, verAluno, removerAluno, buscarCepAluno, limparFormAluno, fecharModal, carregarNotasMatricula });

// Event Listeners
document.getElementById('alunoForm').addEventListener('submit', salvarAluno);
document.getElementById('cpf').addEventListener('input', (e) => { e.target.value = formatCPF(e.target.value); });
document.getElementById('telefone').addEventListener('input', (e) => { e.target.value = formatTelefone(e.target.value); });
document.getElementById('cep').addEventListener('input', (e) => { e.target.value = formatCEP(e.target.value); });

// Inicializar
carregarAlunos();
})();
