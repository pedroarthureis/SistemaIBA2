(function () {
"use strict";
// Matriculas Page Script
let matriculasListData = [];
let carregandoMatriculas = false;
let consultaCursoMatricula = 0;
let materiasDisponiveisMatricula = [];

async function carregarMateriasDoCursoMatricula() {
    const container = document.getElementById('materiasMatricula');
    const cursoId = document.getElementById('cursoId')?.value;
    const aviso = document.getElementById('materiasMatriculaAviso');
    const consulta = ++consultaCursoMatricula;
    if (!container) return;
    container.replaceChildren();
    materiasDisponiveisMatricula = [];
    if (!cursoId) {
        container.textContent = 'Selecione um curso para escolher as matérias.';
        return;
    }
    container.textContent = 'Carregando matérias...';
    try {
        const [curso, materias] = await Promise.all([CursoAPI.getById(cursoId), MateriaAPI.getAll()]);
        if (consulta !== consultaCursoMatricula || !document.getElementById('materiasMatricula')) return;
        const ids = new Set((curso.materiasIds || []).map(Number));
        materiasDisponiveisMatricula = materias.filter(m => ids.has(Number(m.id)));
        renderizarCheckboxes(container, materiasDisponiveisMatricula, new Set(ids));
        if (aviso) aviso.textContent = materiasDisponiveisMatricula.length
            ? 'Desmarque as matérias que este aluno NÃO irá cursar. As notas serão lançadas apenas nas selecionadas.'
            : 'Curso sem matérias. Vá até Cursos e vincule pelo menos uma antes de realizar a matrícula.';
    } catch (error) {
        if (consulta !== consultaCursoMatricula) return;
        container.textContent = 'Erro ao carregar as matérias.';
        showAlert('Erro ao consultar grade: ' + error.message, 'error');
    }
}

function renderizarCheckboxes(container, materias, selecionadas) {
    container.replaceChildren();
    if (!materias.length) {
        container.textContent = 'Nenhuma matéria disponível neste curso.';
        return;
    }
    for (const materia of materias) {
        const label = document.createElement('label'); label.className = 'subject-option';
        const input = document.createElement('input');
        input.type = 'checkbox'; input.value = String(materia.id);
        input.checked = selecionadas.has(Number(materia.id));
        const span = document.createElement('span'); span.textContent = materia.nome;
        label.append(input, span); container.appendChild(label);
    }
}

function materiasMarcadas(containerId) {
    return Array.from(document.querySelectorAll(`#${containerId} input[type="checkbox"]:checked`))
        .map(input => Number(input.value));
}


async function carregarAlunosSelect() {
    try {
        const alunos = await AlunoAPI.getAll();
        const select = document.getElementById('alunoId');
        if (!select) return;
        
        select.innerHTML = '<option value="">Selecione um aluno</option>';
        alunos.forEach(aluno => {
            const option = document.createElement('option');
            option.value = aluno.id;
            option.textContent = `${escapeHtml(aluno.nome)} (${escapeHtml(aluno.matricula || aluno.id)})`;
            select.appendChild(option);
        });
    } catch (error) {
        showAlert('Erro ao carregar alunos: ' + error.message, 'error');
    }
}

async function carregarCursosSelect() {
    try {
        const cursos = await CursoAPI.getAll();
        const select = document.getElementById('cursoId');
        if (!select) return;
        
        select.innerHTML = '<option value="">Selecione um curso</option>';
        cursos.forEach(curso => {
            const option = document.createElement('option');
            option.value = curso.id;
            option.textContent = curso.nome;
            select.appendChild(option);
        });
    } catch (error) {
        showAlert('Erro ao carregar cursos: ' + error.message, 'error');
    }
}

async function carregarMatriculas() {
    if (carregandoMatriculas) {
        console.log('Já carregando matrículas, ignorando...');
        return;
    }
    
    carregandoMatriculas = true;
    
    try {
        const matriculas = await MatriculaAPI.getAll();
        matriculasListData = matriculas;
        renderizarMatriculas(matriculas);
    } catch (error) {
        showAlert('Erro ao carregar matrículas: ' + error.message, 'error');
    } finally {
        carregandoMatriculas = false;
    }
}

function renderizarMatriculas(matriculas) {
    const tbody = document.getElementById('matriculasList');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    if (matriculas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center">Nenhuma matrícula encontrada</td></tr>';
        return;
    }
    
    matriculas.forEach(mat => {
        const media = mat.mediaFinal;
        const finalizada = mat.status === 'APROVADO' || mat.status === 'REPROVADO';
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${escapeHtml(mat.alunoNome)}</strong><br><small>${escapeHtml(mat.alunoMatricula)}</small></td>
            <td>${escapeHtml(mat.cursoNome)}</td>
            <td><span class="subject-count" title="${escapeHtml((mat.materiasNomes || []).join(', '))}">${(mat.materiasIds || []).length} matéria(s)</span></td>
            <td>${mat.ano} - ${mat.semestre}º Semestre</td>
            <td>${formatDate(mat.dataMatricula)}</td>
            <td><span class="status-badge ${getStatusClass(mat.status)}">${escapeHtml(mat.status)}</span></td>
            <td>${finalizada && media != null ? `<span class="status-badge ${getNotaClass(media, 60)}">${media.toFixed(1)}</span>` : '<span class="status-badge status-secondary">Em andamento</span>'}</td>
            <td class="actions">
                <button class="btn btn-sm btn-outline" onclick="verMatricula(${mat.id})" title="Detalhes">
                    <i class="fas fa-eye"></i>
                </button>
                ${mat.status === 'CURSANDO' ? 
                    `<button class="btn btn-sm btn-danger" onclick="cancelarMatricula(${mat.id})" title="Cancelar">
                        <i class="fas fa-times-circle"></i>
                    </button>` : ''}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function buscarMatriculas() {
    const termo = document.getElementById('searchMatricula')?.value.toLowerCase();
    if (!termo) {
        renderizarMatriculas(matriculasListData);
        return;
    }
    
    const filtradas = matriculasListData.filter(m => 
        m.alunoNome.toLowerCase().includes(termo) || 
        m.cursoNome.toLowerCase().includes(termo)
    );
    renderizarMatriculas(filtradas);
}

async function salvarMatricula(event) {
    event.preventDefault();
    
    const alunoId = document.getElementById('alunoId')?.value;
    const cursoId = document.getElementById('cursoId')?.value;
    const ano = parseInt(document.getElementById('ano')?.value);
    const semestre = parseInt(document.getElementById('semestre')?.value);
    const materiasIds = materiasMarcadas('materiasMatricula');
    
    if (!alunoId || !cursoId) {
        showAlert('Selecione aluno e curso!', 'error');
        return;
    }
    
    if (!materiasIds.length) {
        showAlert('Selecione pelo menos uma matéria para este aluno.', 'error');
        return;
    }
    try {
        await MatriculaAPI.create({ 
            alunoId: parseInt(alunoId), 
            cursoId: parseInt(cursoId), 
            ano, 
            semestre,
            materiasIds
        });
        showAlert('Matrícula realizada com sucesso!', 'success');
        limparFormMatricula();
        carregarMatriculas();
    } catch (error) {
        showAlert('Erro ao realizar matrícula: ' + error.message, 'error');
    }
}

async function verMatricula(id) {
    try {
        const matricula = await MatriculaAPI.getById(id);
        const modalBody = document.getElementById('matriculaDetalhes');
        if (!modalBody) return;
        
        // O boletim precisa exibir também as matérias sem notas lançadas.
        const [notas, curso, materias] = await Promise.all([
            NotaAPI.getByMatricula(id), CursoAPI.getById(matricula.cursoId), MateriaAPI.getAll()
        ]);
        const notasPorMateria = new Map();
        for (const materiaId of matricula.materiasIds || []) {
            const materia = materias.find(m => Number(m.id) === Number(materiaId));
            if (materia) notasPorMateria.set(Number(materiaId), {
                materiaNome: materia.nome, materiaId: Number(materiaId), ciclo1: null, ciclo2: null, ciclo3: null
            });
        }
        notas.forEach(nota => {
            if (!notasPorMateria.has(Number(nota.materiaId))) {
                notasPorMateria.set(Number(nota.materiaId), {
                    materiaNome: nota.materiaNome,
                    materiaId: nota.materiaId,
                    ciclo1: null,
                    ciclo2: null,
                    ciclo3: null
                });
            }
            const materiaNotas = notasPorMateria.get(Number(nota.materiaId));
            if (nota.ciclo === 1) materiaNotas.ciclo1 = nota;
            if (nota.ciclo === 2) materiaNotas.ciclo2 = nota;
            if (nota.ciclo === 3) materiaNotas.ciclo3 = nota;
        });
        
        const notasHtml = Array.from(notasPorMateria.values()).map(materia => {
            const nota1 = materia.ciclo1?.valor;
            const nota2 = materia.ciclo2?.valor;
            const nota3 = materia.ciclo3?.valor;
            const valores = [nota1, nota2, nota3].filter(v => v !== undefined && v !== null);
            const media = valores.length > 0 ? valores.reduce((a, b) => a + b, 0) / valores.length : null;
            const statusClass = valores.length < 3 ? 'status-secondary' : media >= 60 ? 'status-success' : 'status-danger';
            
            return `
                <tr>
                    <td>${escapeHtml(materia.materiaNome)}</td>
                    <td>#${materia.materiaId}</td>
                    <td style="text-align: center">${nota1 !== undefined ? nota1.toFixed(1) : '-'}</td>
                    <td style="text-align: center">${nota2 !== undefined ? nota2.toFixed(1) : '-'}</td>
                    <td style="text-align: center">${nota3 !== undefined ? nota3.toFixed(1) : '-'}</td>
                    <td style="text-align: center"><span class="status-badge ${statusClass}">${media === null ? '—' : media.toFixed(1)}</span></td>
                    <td style="text-align: center"><span class="status-badge ${statusClass}">${valores.length < 3 ? 'PENDENTE' : media >= 60 ? 'APROVADO' : 'REPROVADO'}</span></td>
                </tr>
            `;
        }).join('');
        
        modalBody.innerHTML = `
            <div class="form-grid">
                <div><strong>Aluno:</strong><br>${escapeHtml(matricula.alunoNome)}</div>
                <div><strong>Matrícula:</strong><br>${escapeHtml(matricula.alunoMatricula)}</div>
                <div><strong>Curso:</strong><br>${escapeHtml(matricula.cursoNome)}</div>
                <div><strong>Período:</strong><br>${matricula.ano} - ${matricula.semestre}º Semestre</div>
                <div><strong>Data Matrícula:</strong><br>${formatDate(matricula.dataMatricula)}</div>
                <div><strong>Status:</strong><br><span class="status-badge ${getStatusClass(matricula.status)}">${escapeHtml(matricula.status)}</span></div>
                <div><strong>Média Final:</strong><br><span class="status-badge ${matricula.status === 'CURSANDO' ? 'status-secondary' : getNotaClass(matricula.mediaFinal, 60)}">${matricula.status === 'CURSANDO' ? 'Em andamento' : (matricula.mediaFinal ?? 0).toFixed(1)}</span></div>
            </div>
            <hr class="section-rule">
            <div class="section-heading">
                <div><h4>Matérias desta matrícula</h4><p class="form-hint">Escolhidas individualmente para este aluno.</p></div>
                ${matricula.status === 'CURSANDO' ? '<button type="button" id="editarMateriasAluno" class="btn btn-sm btn-outline">Editar matérias</button>' : ''}
            </div>
            <div id="editorMateriasAluno" class="subject-editor" hidden>
                <div id="opcoesMateriasAluno" class="subject-picker"></div>
                <div class="subject-tools"><button type="button" id="salvarMateriasAluno" class="btn btn-primary">Salvar seleção</button><button type="button" id="cancelarEdicaoMaterias" class="btn btn-outline">Cancelar</button></div>
                <p class="form-hint">Só é possível retirar matérias sem notas. Alterações afetam somente esta matrícula.</p>
            </div>
            <hr class="section-rule">
            <h4>Notas por Matéria</h4>
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Matéria</th>
                            <th>ID</th>
                            <th style="text-align: center">1º Ciclo</th>
                            <th style="text-align: center">2º Ciclo</th>
                            <th style="text-align: center">3º Ciclo</th>
                            <th style="text-align: center">Média</th>
                            <th style="text-align: center">Status</th>
                        </tr>
                    </thead>
                    <tbody>${notasHtml || '<tr><td colspan="7" style="text-align: center">Nenhuma nota lançada</td></tr>'}</tbody>
                </table>
            </div>
        `;
        
        const editar = document.getElementById('editarMateriasAluno');
        const editor = document.getElementById('editorMateriasAluno');
        if (editar && editor) {
            editar.addEventListener('click', () => {
                const idsCurso = new Set((curso.materiasIds || []).map(Number));
                renderizarCheckboxes(document.getElementById('opcoesMateriasAluno'),
                    materias.filter(m => idsCurso.has(Number(m.id))),
                    new Set((matricula.materiasIds || []).map(Number)));
                editor.hidden = false;
                editar.hidden = true;
            });
            document.getElementById('cancelarEdicaoMaterias').addEventListener('click', () => {
                editor.hidden = true; editar.hidden = false;
            });
            document.getElementById('salvarMateriasAluno').addEventListener('click', async () => {
                const ids = materiasMarcadas('opcoesMateriasAluno');
                if (!ids.length) { showAlert('Selecione ao menos uma matéria.', 'error'); return; }
                try {
                    const btn = document.getElementById('salvarMateriasAluno');
                    btn.disabled = true;
                    await MatriculaAPI.updateMaterias(id, ids);
                    showAlert('Matérias do aluno atualizadas.', 'success');
                    await carregarMatriculas();
                    await verMatricula(id);
                } catch (error) {
                    showAlert('Não foi possível atualizar: ' + error.message, 'error');
                    const btn = document.getElementById('salvarMateriasAluno');
                    if (btn) btn.disabled = false;
                }
            });
        }
        document.getElementById('matriculaModal').classList.add('active');
    } catch (error) {
        console.error('Erro ao carregar detalhes:', error);
        showAlert('Erro ao carregar detalhes: ' + error.message, 'error');
    }
}

async function cancelarMatricula(id, nomeAluno = "este aluno") {
    if (confirm(`⚠️ Cancelar matrícula do aluno "${nomeAluno}"?\n\nEsta ação não poderá ser desfeita.`)) {
        try {
            await MatriculaAPI.delete(id);
            showAlert('Matrícula cancelada com sucesso!', 'success');
            carregarMatriculas();
        } catch (error) {
            showAlert('Erro ao cancelar: ' + error.message, 'error');
        }
    }
}

function limparFormMatricula() {
    const alunoSelect = document.getElementById('alunoId');
    const cursoSelect = document.getElementById('cursoId');
    const anoInput = document.getElementById('ano');
    const semestreSelect = document.getElementById('semestre');
    
    if (alunoSelect) alunoSelect.selectedIndex = 0;
    if (cursoSelect) cursoSelect.selectedIndex = 0;
    carregarMateriasDoCursoMatricula();
    if (anoInput) anoInput.value = String(new Date().getFullYear());
    if (semestreSelect) semestreSelect.value = '1';
}

function fecharModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
}

// Função auxiliar para classe da nota com limite 60
function getNotaClass(nota, limite = 60) {
    if (nota >= limite) return 'status-success';
    if (nota >= limite - 20) return 'status-warning';
    return 'status-danger';
}

// Registrar funções no window para acesso global
window.verMatricula = verMatricula;
window.cancelarMatricula = cancelarMatricula;
window.fecharModal = fecharModal;


// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { carregarAlunosSelect, carregarCursosSelect, carregarMatriculas, buscarMatriculas, salvarMatricula, verMatricula, cancelarMatricula, limparFormMatricula, fecharModal });

// Event Listeners
const matriculaForm = document.getElementById('matriculaForm');
if (matriculaForm) {
    matriculaForm.addEventListener('submit', salvarMatricula);
}

// Inicializar
const cursoSelectMatricula = document.getElementById('cursoId');
if (cursoSelectMatricula) cursoSelectMatricula.addEventListener('change', carregarMateriasDoCursoMatricula);
document.getElementById('selecionarTodasMaterias')?.addEventListener('click', () => {
    document.querySelectorAll('#materiasMatricula input').forEach(input => input.checked = true);
});
document.getElementById('limparMateriasMatricula')?.addEventListener('click', () => {
    document.querySelectorAll('#materiasMatricula input').forEach(input => input.checked = false);
});
// Um link da tela de notas pode abrir diretamente a matrícula selecionada.
const pendente = typeof sessionStorage !== "undefined" ? sessionStorage.getItem('ibaAbrirMatricula') : null;
if (pendente) {
    sessionStorage.removeItem('ibaAbrirMatricula');
    if (/^\d+$/.test(pendente)) verMatricula(Number(pendente));
}
const anoAtual = document.getElementById('ano');
if (anoAtual && !anoAtual.value) anoAtual.value = String(new Date().getFullYear());
if (document.getElementById('alunoId')) carregarAlunosSelect();
if (document.getElementById('cursoId')) carregarCursosSelect();
if (document.getElementById('matriculasList')) carregarMatriculas();
})();
