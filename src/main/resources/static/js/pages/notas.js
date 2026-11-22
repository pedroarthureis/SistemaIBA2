(function () {
"use strict";
// Notas Page Script
let alunoSelecionado = null;
let matriculaSelecionada = null;
let cursoNotasAtual = null;
let chaveConsultaNotas = 0;

async function initNotas() {
    // Aguardar o DOM estar completamente carregado
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', async () => {
            await carregarAlunos();
            setupEventListeners();
        });
    } else {
        await carregarAlunos();
        setupEventListeners();
    }
}

function setupEventListeners() {
    const vincularBtn = document.getElementById('adicionarMateriaAoCurso');
    if (vincularBtn) vincularBtn.addEventListener('click', vincularMateriaAoCurso);
    const notaForm = document.getElementById('notaForm');
    if (notaForm) {
        notaForm.addEventListener('submit', salvarNota);
    }
    
    const filtroAluno = document.getElementById('filtroAluno');
    if (filtroAluno) {
        filtroAluno.addEventListener('change', carregarMatriculasDoAluno);
    }
    
    const filtroMatricula = document.getElementById('filtroMatricula');
    if (filtroMatricula) {
        filtroMatricula.addEventListener('change', carregarNotasDaMatricula);
    }
}

async function carregarAlunos() {
    const selectAluno = document.getElementById('filtroAluno');
    if (!selectAluno) {
        console.error('Elemento filtroAluno não encontrado');
        return;
    }
    
    try {
        const alunos = await AlunoAPI.getAll();
        
        selectAluno.innerHTML = '<option value="">Selecione um aluno</option>';
        alunos.forEach(aluno => {
            const option = document.createElement('option');
            option.value = aluno.id;
            option.textContent = `${aluno.nome} (${aluno.matricula || aluno.id})`;
            selectAluno.appendChild(option);
        });
        
        const matriculaId = sessionStorage.getItem('matriculaSelecionadaId');
        if (matriculaId) {
            sessionStorage.removeItem('matriculaSelecionadaId');
            await selecionarMatriculaPorId(matriculaId);
        }
    } catch (error) {
        console.error('Erro ao carregar alunos:', error);
        showAlert('Erro ao carregar alunos: ' + error.message, 'error');
    }
}

async function selecionarMatriculaPorId(matriculaId) {
    try {
        const matricula = await MatriculaAPI.getById(matriculaId);
        if (matricula && matricula.alunoId) {
            const selectAluno = document.getElementById('filtroAluno');
            if (selectAluno) selectAluno.value = matricula.alunoId;
            
            await carregarMatriculasDoAluno();
            
            const selectMatricula = document.getElementById('filtroMatricula');
            if (selectMatricula) selectMatricula.value = matriculaId;
            
            await carregarNotasDaMatricula();
        }
    } catch (error) {
        console.error('Erro ao selecionar matrícula:', error);
    }
}

async function carregarMatriculasDoAluno() {
    const selectAluno = document.getElementById('filtroAluno');
    const alunoId = selectAluno?.value;
    matriculaSelecionada = null;
    cursoNotasAtual = null;
    ++chaveConsultaNotas;
    const painelVinculo = document.getElementById('vinculoMateriaPanel');
    if (painelVinculo) painelVinculo.hidden = true;
    const materiaSelect = document.getElementById('materiaNota');
    if (materiaSelect) materiaSelect.innerHTML = '<option value="">Selecione uma matrícula primeiro</option>';
    
    const selectMatricula = document.getElementById('filtroMatricula');
    const infoPanel = document.getElementById('alunoInfoPanel');
    const notasTable = document.getElementById('notasTable');
    
    if (!alunoId) {
        if (selectMatricula) selectMatricula.innerHTML = '<option value="">Selecione um aluno primeiro</option>';
        if (infoPanel) infoPanel.style.display = 'none';
        if (notasTable) notasTable.innerHTML = '<tr><td colspan="7" style="text-align: center">Selecione um aluno e matrícula</td></tr>';
        return;
    }
    
    try {
        const aluno = await AlunoAPI.getById(parseInt(alunoId));
        alunoSelecionado = aluno;
        
        if (infoPanel) infoPanel.style.display = 'block';
        const alunoInfo = document.getElementById('alunoInfo');
        if (alunoInfo) {
            alunoInfo.innerHTML = `
                <div><strong>Nome:</strong> ${escapeHtml(aluno.nome)}</div>
                <div><strong>Matrícula:</strong> ${escapeHtml(aluno.matricula || aluno.id)}</div>
                <div><strong>Curso Atual:</strong> ${escapeHtml(aluno.cursoAtual?.nome || 'Não matriculado')}</div>
            `;
        }
        
        const matriculas = await MatriculaAPI.getByAluno(parseInt(alunoId));
        if (!selectMatricula) return;
        
        selectMatricula.innerHTML = '<option value="">Selecione uma matrícula</option>';
        
        matriculas.forEach(mat => {
            const option = document.createElement('option');
            option.value = mat.id;
            option.textContent = `${mat.cursoNome} - ${mat.ano}/${mat.semestre} (${mat.status})`;
            selectMatricula.appendChild(option);
        });
    } catch (error) {
        console.error('Erro ao carregar matrículas:', error);
        showAlert('Erro ao carregar matrículas: ' + error.message, 'error');
    }
}

async function carregarNotasDaMatricula() {
    const selectMatricula = document.getElementById('filtroMatricula');
    const matriculaId = selectMatricula?.value;
    const selectMateria = document.getElementById('materiaNota');
    const notasTable = document.getElementById('notasTable');
    const vinculoPanel = document.getElementById('vinculoMateriaPanel');
    const aviso = document.getElementById('notaFormAviso');
    const consultaId = ++chaveConsultaNotas;
    if (!matriculaId) {
        matriculaSelecionada = null;
        cursoNotasAtual = null;
        if (vinculoPanel) vinculoPanel.hidden = true;
        if (selectMateria) selectMateria.innerHTML = '<option value="">Selecione uma matrícula primeiro</option>';
        if (notasTable) notasTable.innerHTML = '<tr><td colspan="7" style="text-align: center">Selecione um aluno e matrícula</td></tr>';
        if (aviso) aviso.textContent = 'Selecione uma matrícula para consultar suas matérias.';
        return;
    }
    if (notasTable) notasTable.innerHTML = '<tr><td colspan="7">Carregando notas...</td></tr>';
    try {
        const matricula = await MatriculaAPI.getById(matriculaId);
        const [curso, materias, notas] = await Promise.all([
            CursoAPI.getById(matricula.cursoId),
            MateriaAPI.getAll(),
            NotaAPI.getByMatricula(matriculaId)
        ]);
        if (consultaId !== chaveConsultaNotas || !document.getElementById('notasTable')) return;
        matriculaSelecionada = Number(matriculaId);
        cursoNotasAtual = curso;
        document.getElementById('matriculaAtualId').value = matriculaId;
        const gradeCursoIds = (curso.materiasIds || []).map(Number);
        const materiaIds = (matricula.materiasIds || []).map(Number);
        const materiasVinculadas = materias.filter(m => materiaIds.includes(Number(m.id)));
        const materiasDisponiveis = materias.filter(m => !gradeCursoIds.includes(Number(m.id)));
        const editarGrade = document.getElementById('editarGradeAluno');
        if (editarGrade) {
            editarGrade.hidden = matricula.status !== 'CURSANDO';
            editarGrade.onclick = () => {
                sessionStorage.setItem('ibaAbrirMatricula', matriculaId);
                window.location.hash = 'matriculas';
            };
        }
        if (selectMateria) {
            selectMateria.replaceChildren(new Option('Selecione uma matéria', ''));
            for (const materia of materiasVinculadas) selectMateria.add(new Option(materia.nome, materia.id));
        }
        const novoSelect = document.getElementById('novaMateriaVinculo');
        if (novoSelect) {
            novoSelect.replaceChildren(new Option('Selecione uma matéria', ''));
            for (const materia of materiasDisponiveis) novoSelect.add(new Option(materia.nome, materia.id));
        }
        if (vinculoPanel) {
            vinculoPanel.hidden = matricula.status !== 'CURSANDO' || (materiasDisponiveis.length === 0 && materiasVinculadas.length > 0);
            const mensagem = document.getElementById('vinculoMateriaMensagem');
            if (mensagem) mensagem.textContent = materiasVinculadas.length
                ? `O curso ${curso.nome} possui ${materiasVinculadas.length} matéria(s) vinculada(s). Você pode adicionar outra matéria aqui.`
                : `O curso ${curso.nome} ainda não tem matérias vinculadas. Cadastre ou vincule uma abaixo para liberar o lançamento de notas.`;
            const btn = document.getElementById('adicionarMateriaAoCurso');
            if (btn) btn.disabled = materiasDisponiveis.length === 0;
        }
        if (aviso) aviso.textContent = materiasVinculadas.length
            ? `Matérias desta matrícula: ${materiasVinculadas.length}. Clique em Lançar na tabela ou use o formulário.`
            : 'Esta matrícula não tem matérias selecionadas. Edite a seleção do aluno ou vincule matérias ao curso.';
        const salvarNotaBtn = document.querySelector('#notaForm button[type="submit"]');
        if (salvarNotaBtn) salvarNotaBtn.disabled = materiasVinculadas.length === 0 || matricula.status === 'TRANCADA';
        if (matricula.status === 'TRANCADA' && aviso) aviso.textContent = 'Matrícula trancada: lançamento e edição de notas bloqueados.';
        if (!materiasVinculadas.length) {
            if (notasTable) notasTable.innerHTML = '<tr><td colspan="7" style="text-align:center">Este curso não possui matérias vinculadas. Utilize o painel “Matérias do curso” acima para vincular.</td></tr>';
            return;
        }
        if (notasTable) notasTable.replaceChildren();
        for (const materia of materiasVinculadas) {
            const materiaId = Number(materia.id);
            const notasMateria = notas.filter(n => Number(n.materiaId) === materiaId);
            const valores = [1, 2, 3].map(ciclo => notasMateria.find(n => n.ciclo === ciclo));
            const existentes = valores.filter(n => n && Number.isFinite(n.valor));
            const media = existentes.length ? existentes.reduce((s,n) => s + n.valor,0)/existentes.length : null;
            const status = existentes.length < 3 ? 'PENDENTE' : media >= 60 ? 'APROVADO' : 'REPROVADO';
            const statusClass = status === 'PENDENTE' ? 'status-secondary' : status === 'APROVADO' ? 'status-success' : 'status-danger';
            const tr = document.createElement('tr');
            const title = document.createElement('td');
            const strong = document.createElement('strong'); strong.textContent = materia.nome; title.appendChild(strong); tr.appendChild(title);
            const idCell = document.createElement('td'); idCell.textContent = '#' + materiaId; tr.appendChild(idCell);
            for (let i = 0; i < 3; i++) {
                const td = document.createElement('td'); td.style.textAlign = 'center';
                const nota = valores[i];
                const btn = document.createElement('button');
                btn.type = 'button'; btn.disabled = matricula.status === 'TRANCADA'; btn.className = nota ? 'btn btn-sm btn-outline' : 'btn btn-sm btn-primary';
                btn.textContent = nota ? nota.valor.toFixed(1) + ' ✎' : 'Lançar';
                btn.title = nota ? `Editar nota do ${i+1}º ciclo` : `Lançar ${i+1}º ciclo`;
                btn.addEventListener('click', () => lancarNota(materiaId, i+1, nota?.valor));
                td.appendChild(btn); tr.appendChild(td);
            }
            const mediaTd = document.createElement('td'); mediaTd.style.textAlign = 'center';
            mediaTd.textContent = media === null ? '—' : media.toFixed(1);
            if (existentes.length < 3) mediaTd.title = 'Média parcial; faltam notas';
            tr.appendChild(mediaTd);
            const statusTd = document.createElement('td'); statusTd.style.textAlign='center';
            const badge = document.createElement('span'); badge.className = 'status-badge ' + statusClass; badge.textContent = status;
            statusTd.appendChild(badge); tr.appendChild(statusTd);
            notasTable.appendChild(tr);
        }
    } catch (error) {
        if (consultaId !== chaveConsultaNotas) return;
        if (notasTable) notasTable.innerHTML = '<tr><td colspan="7">Erro ao carregar as notas. Tente novamente.</td></tr>';
        showAlert('Erro ao carregar dados: ' + error.message, 'error');
    }
}

async function vincularMateriaAoCurso() {
    const materiaId = Number(document.getElementById('novaMateriaVinculo')?.value);
    const btn = document.getElementById('adicionarMateriaAoCurso');
    if (!materiaId || !cursoNotasAtual || !matriculaSelecionada) {
        showAlert('Selecione uma matéria e uma matrícula antes de vincular.', 'error'); return;
    }
    if (!confirm(`Vincular ao curso "${cursoNotasAtual.nome}" e à matrícula selecionada? A grade do curso muda para todos, mas somente este aluno receberá a nova matéria na sua seleção individual.`)) return;
    if (btn) btn.disabled = true;
    try {
        // Busca a versão mais recente do curso para não perder outras vinculações.
        const atual = await CursoAPI.getById(cursoNotasAtual.id);
        const ids = new Set((atual.materiasIds || []).map(Number));
        ids.add(materiaId);
        await CursoAPI.update(atual.id, { nome: atual.nome, materiasIds: Array.from(ids) });
        const mat = await MatriculaAPI.getById(matriculaSelecionada);
        const selecionadas = new Set((mat.materiasIds || []).map(Number));
        selecionadas.add(materiaId);
        await MatriculaAPI.updateMaterias(matriculaSelecionada, Array.from(selecionadas));
        showAlert('Matéria vinculada ao curso e à matrícula deste aluno.', 'success');
        await carregarNotasDaMatricula();
    } catch (error) {
        showAlert('A associação não foi concluída: ' + error.message + '. Confira a grade antes de tentar novamente.', 'error');
    } finally {
        const select = document.getElementById('novaMateriaVinculo');
        if (btn && select) btn.disabled = select.options.length <= 1;
    }
}

function lancarNota(materiaId, ciclo, valorAtual = null) {
    const selectMateria = document.getElementById('materiaNota');
    const cicloSelect = document.getElementById('ciclo');
    const valorInput = document.getElementById('valorNota');
    if (selectMateria) selectMateria.value = String(materiaId);
    if (cicloSelect) cicloSelect.value = String(ciclo);
    if (valorInput) { valorInput.value = valorAtual === null ? '' : String(valorAtual); valorInput.focus(); }
    document.getElementById('notaForm')?.scrollIntoView({behavior: 'smooth'});
}

function editarNota(id, ciclo, valor) {
    // Mantido por compatibilidade. Use os botões da própria tabela para selecionar a matéria.
    showAlert('Clique na nota da tabela para editar a matéria e o ciclo corretos.', 'info');
}

async function salvarNota(event) {
    event.preventDefault();
    
    if (!matriculaSelecionada) {
        showAlert('❌ Selecione um aluno e matrícula primeiro!', 'error');
        return;
    }
    
    const materiaId = document.getElementById('materiaNota')?.value;
    const ciclo = parseInt(document.getElementById('ciclo')?.value);
    const textoValor = String(document.getElementById('valorNota')?.value || '').trim().replace(',', '.');
    const valor = /^(?:\d{1,3})(?:\.\d+)?$/.test(textoValor) ? Number(textoValor) : NaN;
    
    if (!materiaId) {
        showAlert('❌ Selecione uma matéria!', 'error');
        return;
    }
    
    if (isNaN(valor) || valor < 0 || valor > 100) {
        showAlert('❌ Nota inválida! Digite um valor entre 0 e 100.', 'error');
        return;
    }
    
    const notaArredondada = Math.round(valor * 10) / 10;
    
    try {
        const materia = await MateriaAPI.getById(parseInt(materiaId));
        const materiaNome = materia ? materia.nome : 'Matéria';
        
        await NotaAPI.save({
            matriculaId: matriculaSelecionada,
            materiaId: parseInt(materiaId),
            ciclo: ciclo,
            valor: notaArredondada
        });
        
        showAlert(`✅ Nota ${notaArredondada.toFixed(1)} lançada com sucesso!\n📚 ${materiaNome} - ${ciclo}º ciclo`, 'success');
        
        const valorInput = document.getElementById('valorNota');
        if (valorInput) valorInput.value = '';
        
        await carregarNotasDaMatricula();
        
    } catch (error) {
        showAlert('❌ Erro ao salvar nota: ' + error.message, 'error');
    }
}

// Registrar funções no window para acesso global
window.carregarMatriculasDoAluno = carregarMatriculasDoAluno;
window.carregarNotasDaMatricula = carregarNotasDaMatricula;
window.lancarNota = lancarNota;
window.editarNota = editarNota;
window.salvarNota = salvarNota;
window.vincularMateriaAoCurso = vincularMateriaAoCurso;

// Inicializar
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initNotas);
} else {
    initNotas();
}
// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { initNotas, carregarAlunos, selecionarMatriculaPorId, carregarMatriculasDoAluno, carregarNotasDaMatricula, lancarNota, editarNota, salvarNota });


})();
