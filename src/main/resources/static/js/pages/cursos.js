(function () {
"use strict";
// Cursos Page Script

// Uma matéria criada isoladamente não passa a integrar automaticamente nenhum curso.
// As caixas de seleção deixam essa associação explícita e acessível.
async function carregarMateriasSelect(idsSelecionados = []) {
    const container = document.getElementById('materiasSelect');
    if (!container) return;
    try {
        const materias = await MateriaAPI.getAll();
        container.replaceChildren();
        if (!materias.length) {
            container.textContent = 'Nenhuma matéria cadastrada. Acesse Matérias antes de vincular.';
            return;
        }
        for (const materia of materias) {
            const label = document.createElement('label');
            label.className = 'subject-option';
            const input = document.createElement('input');
            input.type = 'checkbox';
            input.name = 'materiaDoCurso';
            input.value = String(materia.id);
            input.checked = idsSelecionados.map(Number).includes(Number(materia.id));
            const span = document.createElement('span');
            span.textContent = materia.nome;
            label.append(input, span);
            container.appendChild(label);
        }
    } catch (error) {
        container.textContent = 'Não foi possível carregar as matérias.';
        showAlert('Erro ao carregar matérias: ' + error.message, 'error');
    }
}

function materiasSelecionadas() {
    return Array.from(document.querySelectorAll('#materiasSelect input[name="materiaDoCurso"]:checked'))
        .map(input => Number(input.value));
}

async function carregarCursos() {
    try {
        const cursos = await CursoAPI.getAll();
        const tbody = document.getElementById('cursosList');
        tbody.innerHTML = '';
        
        if (cursos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align: center">Nenhum curso cadastrado</td></tr>';
            return;
        }
        
        for (const curso of cursos) {
            const materiasHtml = (curso.materiasNomes || []).map(m => 
                `<span class="status-badge status-secondary">${escapeHtml(m)}</span>`
            ).join(' ') || '<span class="status-badge status-warning">Sem matérias — vincule para lançar notas</span>';
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${curso.id}</td>
                <td><strong>${escapeHtml(curso.nome)}</strong></td>
                <td style="max-width: 400px;">${materiasHtml}</td>
                <td class="actions">
                    <button class="btn btn-sm btn-outline" onclick="editarCurso(${curso.id})" title="Editar curso" aria-label="Editar curso ${curso.id}">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="removerCurso(${curso.id})" title="Remover curso" aria-label="Remover curso ${curso.id}">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        }
    } catch (error) {
        showAlert('Erro ao carregar cursos: ' + error.message, 'error');
    }
}

async function salvarCurso(event) {
    event.preventDefault();
    
    const id = document.getElementById('editCursoId').value;
    const nome = document.getElementById('nome').value;
    const materiasIds = materiasSelecionadas();
    
    if (!nome.trim()) {
        showAlert('Digite o nome do curso!', 'error');
        return;
    }
    
    if (!materiasIds.length && !confirm('Este curso ficará sem matérias vinculadas e não permitirá lançar notas. Deseja continuar?')) return;
    const cursoData = { nome: nome.trim(), materiasIds };
    
    try {
        if (id) {
            await CursoAPI.update(id, cursoData);
            showAlert('Curso atualizado com sucesso!');
        } else {
            await CursoAPI.create(cursoData);
            showAlert('Curso cadastrado com sucesso!');
        }
        limparFormCurso();
        await carregarCursos();
    } catch (error) {
        showAlert('Erro ao salvar: ' + error.message, 'error');
    }
}

async function editarCurso(id) {
    try {
        const curso = await CursoAPI.getById(id);
        document.getElementById('editCursoId').value = curso.id;
        document.getElementById('nome').value = curso.nome;
        
        // Selecionar matérias do curso
        await carregarMateriasSelect(curso.materiasIds || []);
        const titulo = document.getElementById('tituloFormCurso');
        if (titulo) titulo.textContent = 'Editar curso e vincular matérias';
        
        document.getElementById('cursoForm').scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showAlert('Erro ao carregar curso: ' + error.message, 'error');
    }
}

async function removerCurso(id, nome = "este registro") {
    if (confirm(`⚠️ Remover o curso "${nome}"?\n\nA exclusão será impedida se houver matrículas vinculadas.`)) {
        try {
            await CursoAPI.delete(id);
            showAlert('Curso removido com sucesso!');
            carregarCursos();
        } catch (error) {
            showAlert('Erro ao remover: ' + error.message, 'error');
        }
    }
}

function limparFormCurso() {
    document.getElementById('editCursoId').value = '';
    document.getElementById('nome').value = '';
    document.querySelectorAll('#materiasSelect input[name="materiaDoCurso"]').forEach(opt => opt.checked = false);
    const titulo = document.getElementById('tituloFormCurso');
    if (titulo) titulo.textContent = 'Novo Curso';
}


// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { carregarMateriasSelect, carregarCursos, salvarCurso, editarCurso, removerCurso, limparFormCurso });

// Event Listeners
document.getElementById('cursoForm').addEventListener('submit', salvarCurso);

// Inicializar
carregarMateriasSelect();
carregarCursos();
})();
