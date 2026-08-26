// ============================================
// API CONFIGURATION
// ============================================

// 🔥 DETECTA O AMBIENTE AUTOMATICAMENTE
const API_BASE_URL = (() => {
    // Se não for localhost, está em produção (Render)
    if (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
        // No Render, o backend está no mesmo domínio
        return `${window.location.origin}/api`;
    }
    // Desenvolvimento local
    return 'http://localhost:8181/api';
})();

console.log('🔧 API_BASE_URL:', API_BASE_URL);

// Helper para fazer requisições
async function apiRequest(endpoint, method = 'GET', data = null) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        }
    };
    
    if (data) {
        options.body = JSON.stringify(data);
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || `Erro ${response.status}`);
        }
        
        if (method === 'DELETE' || response.status === 204) {
            return null;
        }
        
        return await response.json();
    } catch (error) {
        console.error(`API Error (${endpoint}):`, error);
        throw error;
    }
}

// ============================================
// ALUNOS API
// ============================================
const AlunoAPI = {
    getAll: () => apiRequest('/alunos'),
    getById: (id) => apiRequest(`/alunos/${id}`),
    search: (nome) => apiRequest(`/alunos/search?nome=${encodeURIComponent(nome)}`),
    create: (data) => apiRequest('/alunos', 'POST', data),
    update: (id, data) => apiRequest(`/alunos/${id}`, 'PUT', data),
    updateStatus: (id, status) => apiRequest(`/alunos/${id}/status?status=${status}`, 'PATCH'),
    delete: (id) => apiRequest(`/alunos/${id}`, 'DELETE')
};

// ============================================
// CURSOS API
// ============================================
const CursoAPI = {
    getAll: () => apiRequest('/cursos'),
    getById: (id) => apiRequest(`/cursos/${id}`),
    create: (data) => apiRequest('/cursos', 'POST', data),
    update: (id, data) => apiRequest(`/cursos/${id}`, 'PUT', data),
    delete: (id) => apiRequest(`/cursos/${id}`, 'DELETE')
};

// ============================================
// MATÉRIAS API
// ============================================
const MateriaAPI = {
    getAll: () => apiRequest('/materias'),
    getById: (id) => apiRequest(`/materias/${id}`),
    create: (data) => apiRequest('/materias', 'POST', data),
    update: (id, data) => apiRequest(`/materias/${id}`, 'PUT', data),
    delete: (id) => apiRequest(`/materias/${id}`, 'DELETE')
};

// ============================================
// PROFESSORES API
// ============================================
const ProfessorAPI = {
    getAll: () => apiRequest('/professores'),
    getById: (id) => apiRequest(`/professores/${id}`),
    create: (data) => apiRequest('/professores', 'POST', data),
    update: (id, data) => apiRequest(`/professores/${id}`, 'PUT', data),
    delete: (id) => apiRequest(`/professores/${id}`, 'DELETE')
};

// ============================================
// MATRÍCULAS API
// ============================================
const MatriculaAPI = {
    getAll: () => apiRequest('/matriculas'),
    getById: (id) => apiRequest(`/matriculas/${id}`),
    getByAluno: (alunoId) => apiRequest(`/matriculas/aluno/${alunoId}`),
    getByCursoAndPeriodo: (cursoId, ano, semestre) => apiRequest(`/matriculas/curso/${cursoId}/ano/${ano}/semestre/${semestre}`),
    getAtivasByCurso: (cursoId) => apiRequest(`/matriculas/curso/${cursoId}/ativas`),
    create: (data) => apiRequest('/matriculas', 'POST', data),
    updateStatus: (id, status) => apiRequest(`/matriculas/${id}/status?status=${status}`, 'PATCH'),
    delete: (id) => apiRequest(`/matriculas/${id}`, 'DELETE')
};

// ============================================
// NOTAS API
// ============================================
const NotaAPI = {
    getAll: () => apiRequest('/notas'),
    getByMatricula: (matriculaId) => apiRequest(`/notas/matricula/${matriculaId}`),
    getByAluno: (alunoId) => apiRequest(`/notas/aluno/${alunoId}`),
    getByMateria: (materiaId) => apiRequest(`/notas/materia/${materiaId}`),
    save: (data) => apiRequest('/notas', 'POST', data),
    delete: (id) => apiRequest(`/notas/${id}`, 'DELETE')
};

// ============================================
// DOAÇÕES API
// ============================================
const DoacaoAPI = {
    getAll: () => apiRequest('/doacoes'),
    getById: (id) => apiRequest(`/doacoes/${id}`),
    create: (data) => apiRequest('/doacoes', 'POST', data),
    update: (id, data) => apiRequest(`/doacoes/${id}`, 'PUT', data),
    delete: (id) => apiRequest(`/doacoes/${id}`, 'DELETE'),
    getDashboard: () => apiRequest('/doacoes/dashboard')
};

// ============================================
// DASHBOARD API
// ============================================
const DashboardAPI = {
    getData: () => apiRequest('/dashboard')
};

// ============================================
// RELATÓRIOS API
// ============================================
const RelatorioAPI = {
    getBoletim: (alunoId) => apiRequest(`/relatorios/boletim/${alunoId}`),
    getRendimentoCursos: () => apiRequest('/relatorios/rendimento-cursos'),
    getTaxaAprovacao: () => apiRequest('/relatorios/taxa-aprovacao')
};

// ============================================
// ENDEREÇO API (ViaCEP)
// ============================================
const EnderecoAPI = {
    buscarCep: (cep) => apiRequest(`/endereco/cep/${cep}`)
};