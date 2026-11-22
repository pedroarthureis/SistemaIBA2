(function () {
"use strict";
// Materias Page Script
let materiasListData = [];

async function carregarMaterias() {
    try {
        const materias = await MateriaAPI.getAll();
        materiasListData = materias;
        renderizarMaterias(materias);
    } catch (error) {
        showAlert('Erro ao carregar matérias: ' + error.message, 'error');
    }
}

function renderizarMaterias(materias) {
    const tbody = document.getElementById('materiasList');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    if (materias.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center">Nenhuma matéria cadastrada</td></tr>';
        return;
    }
    
    materias.forEach(materia => {
        const media = materia.mediaGeralNotas || 0;
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>#${materia.id}</strong></td>
            <td>${escapeHtml(materia.nome)}</td>
            <td><span class="status-badge status-info">${materia.quantidadeCursos || 0} curso(s)</span></td>
            <td><span class="status-badge ${getNotaClass(media)}">${media.toFixed(1)}</span></td>
            <td class="actions">
                <button class="btn btn-sm btn-outline" onclick="editarMateria(${materia.id})" title="Editar matéria" aria-label="Editar matéria ${materia.id}">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="removerMateria(${materia.id})" title="Remover matéria" aria-label="Remover matéria ${materia.id}">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function buscarMaterias() {
    const termo = document.getElementById('searchMateria')?.value.toLowerCase();
    if (!termo) {
        renderizarMaterias(materiasListData);
        return;
    }
    
    const filtradas = materiasListData.filter(m => 
        m.nome.toLowerCase().includes(termo)
    );
    renderizarMaterias(filtradas);
}

async function salvarMateria(event) {
    event.preventDefault();
    
    const id = document.getElementById('editMateriaId')?.value;
    const nome = document.getElementById('nome')?.value;
    
    if (!nome) {
        showAlert('Preencha o nome da matéria!', 'error');
        return;
    }
    
    try {
        if (id) {
            await MateriaAPI.update(id, { nome });
            showAlert('Matéria atualizada com sucesso!');
        } else {
            const result = await MateriaAPI.create({ nome });
            showAlert(`Matéria "${result.nome}" cadastrada com sucesso! ID: ${result.id}`);
        }
        limparFormMateria();
        carregarMaterias();
    } catch (error) {
        showAlert('Erro ao salvar: ' + error.message, 'error');
    }
}

async function editarMateria(id) {
    try {
        const materia = await MateriaAPI.getById(id);
        document.getElementById('editMateriaId').value = materia.id;
        document.getElementById('nome').value = materia.nome;
        
        document.getElementById('materiaForm').scrollIntoView({ behavior: 'smooth' });
    } catch (error) {
        showAlert('Erro ao carregar matéria: ' + error.message, 'error');
    }
}

async function removerMateria(id, nome = "este registro") {
    if (confirm(`⚠️ Remover a matéria "${nome}"?\n\nNão é possível excluir matérias vinculadas a cursos ou com notas registradas.`)) {
        try {
            await MateriaAPI.delete(id);
            showAlert('Matéria removida com sucesso!');
            carregarMaterias();
        } catch (error) {
            showAlert('Erro ao remover: ' + error.message, 'error');
        }
    }
}

function limparFormMateria() {
    const editId = document.getElementById('editMateriaId');
    const nomeInput = document.getElementById('nome');
    
    if (editId) editId.value = '';
    if (nomeInput) nomeInput.value = '';
}


// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { carregarMaterias, buscarMaterias, salvarMateria, editarMateria, removerMateria, limparFormMateria });

// Event Listeners
const materiaForm = document.getElementById('materiaForm');
if (materiaForm) {
    materiaForm.addEventListener('submit', salvarMateria);
}

// Inicializar
if (document.getElementById('materiasList')) {
    carregarMaterias();
}
})();
