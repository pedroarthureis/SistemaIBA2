const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = path.join(__dirname, '../../src/main/resources/static/js/pages');
const pages = ['dashboard', 'alunos', 'cursos', 'materias', 'professores', 'matriculas', 'notas', 'doacoes'];

test('todas as paginas possuem escopo isolado para evitar redeclaracao global', () => {
  pages.forEach(page => {
    const js = fs.readFileSync(path.join(base, `${page}.js`), 'utf8');
    assert.match(js, /^\(function \(\) \{/);
    assert.match(js, /\}\)\(\);\s*$/);
  });
});

test('nao existem handlers de remocao com nomes interpolados em strings JS', () => {
  pages.forEach(page => {
    const js = fs.readFileSync(path.join(base, `${page}.js`), 'utf8');
    assert.doesNotMatch(js, /onclick="(?:removerCurso|removerMateria|removerProfessor|cancelarMatricula)\([^)]*,\s*'/);
  });
});

const vm = require('node:vm');
test('reabrir matérias e matrículas não causa erro de redeclaração', () => {
  const fakeDocument = { getElementById: () => null };
  const context = vm.createContext({ window: {}, document: fakeDocument });
  for (const page of ['materias', 'matriculas']) {
    const script = fs.readFileSync(path.join(base, `${page}.js`), 'utf8');
    assert.doesNotThrow(() => vm.runInContext(script, context));
    assert.doesNotThrow(() => vm.runInContext(script, context));
  }
});
