(function () {
"use strict";
// Dashboard Page Script

async function initDashboard() {
    try {
        const data = await DashboardAPI.getData();
        
        // Atualizar cards
        document.getElementById('totalAlunos').innerText = data.totalAlunos || 0;
        document.getElementById('alunosAtivos').innerText = data.totalAlunosAtivos || 0;
        document.getElementById('totalCursos').innerText = data.totalCursos || 0;
        document.getElementById('totalMaterias').innerText = data.totalMaterias || 0;
        document.getElementById('totalProfessores').innerText = data.totalProfessores || 0;
        
        // Distribuição por curso
        const distribuicaoBody = document.getElementById('distribuicaoBody');
        distribuicaoBody.innerHTML = '';
        
        const totalAlunos = data.totalAlunos || 1;
        for (const [curso, quantidade] of Object.entries(data.distribuicaoAlunosPorCurso || {})) {
            const percentual = (quantidade / totalAlunos * 100).toFixed(1);
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${escapeHtml(curso)}</strong></td>
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
            distribuicaoBody.appendChild(tr);
        }
        
        // Desempenho por curso (média de notas dos alunos aprovados)
        const desempenhoBody = document.getElementById('desempenhoBody');
        desempenhoBody.innerHTML = '';
        
        for (const curso of (data.desempenhoPorCurso || [])) {
            const media = curso.media || 0;
            const status = media >= 60 ? '✅ Bom' : media >= 40 ? '⚠️ Regular' : '❌ Crítico';
            const statusClass = media >= 60 ? 'status-success' : media >= 40 ? 'status-warning' : 'status-danger';
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${escapeHtml(curso.curso)}</strong></td>
                <td><span class="status-badge ${statusClass}">${media.toFixed(1)}</span></td>
                <td>${status}</td>
            `;
            desempenhoBody.appendChild(tr);
        }
        
        // Alunos recentes
        const recentesBody = document.getElementById('alunosRecentesBody');
        recentesBody.innerHTML = '';
        
        for (const aluno of (data.alunosRecentes || [])) {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(aluno.matricula || aluno.id)}</td>
                <td><strong>${escapeHtml(aluno.nome)}</strong></td>
                <td>${escapeHtml(aluno.curso || 'Não matriculado')}</td>
                <td>${formatDate(aluno.dataCadastro)}</td>
            `;
            recentesBody.appendChild(tr);
        }
        
        // Adicionar navegação nos cards
        document.querySelectorAll('.card[data-page]').forEach(card => {
            card.addEventListener('click', () => {
                const page = card.dataset.page;
                document.querySelector(`.menu-item[data-page="${page}"]`).click();
            });
            card.addEventListener('keydown', e => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    card.click();
                }
            });
        });
        
    } catch (error) {
        console.error('Erro ao carregar dashboard:', error);
        showAlert('Erro ao carregar dados do dashboard', 'error');
    }
}


// Funcoes usadas pelos botoes e buscas desta tela.
Object.assign(window, { initDashboard });

// Inicializar quando a página carregar
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDashboard);
} else {
    initDashboard();
}
})();
