const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const root = path.resolve(__dirname, '../../src/main/resources/static');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');

test('formulário da matrícula permite seleção individual de matérias', () => {
  const html = read('pages/matriculas.html');
  assert.match(html, /id="materiasMatricula"/);
  assert.match(html, /id="selecionarTodasMaterias"/);
  assert.match(read('js/pages/matriculas.js'), /materiasIds/);
});

test('tela de notas lista apenas matérias da matrícula e edita a seleção', () => {
  const js = read('js/pages/notas.js');
  assert.match(js, /const materiaIds = \(matricula\.materiasIds \|\| \[\]\)/);
  assert.match(js, /MatriculaAPI\.updateMaterias/);
  assert.match(read('pages/notas.html'), /id="editarGradeAluno"/);
});

test('endpoint da seleção individual fica declarado no cliente', () => {
  assert.match(read('js/api.js'), /matriculas\/\$\{id\}\/materias/);
});
