const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const test = require('node:test');

const js = fs.readFileSync(path.resolve(__dirname, '../../src/main/resources/static/js/pages/matriculas.js'), 'utf8');

test('ao criar matrícula, envia somente as matérias marcadas para o backend', async () => {
  const submitted = [];
  const fields = new Map([
    ['matriculaForm', { addEventListener() {} }],
    ['alunoId', { value: '10', selectedIndex: 0, appendChild() {} }],
    ['cursoId', { value: '20', selectedIndex: 0, addEventListener() {}, appendChild() {} }],
    ['ano', { value: '2026' }],
    ['semestre', { value: '2' }],
  ]);
  const context = {
    window: {}, console,
    document: {
      getElementById: id => fields.get(id) ?? null,
      querySelectorAll: selector => selector === '#materiasMatricula input[type="checkbox"]:checked'
        ? [{ value: '7' }, { value: '9' }] : [],
      createElement: () => ({ value: '', textContent: '' })
    },
    AlunoAPI: { getAll: async () => [] },
    CursoAPI: { getAll: async () => [] },
    MatriculaAPI: { create: async data => submitted.push(data) },
    showAlert() {},
  };
  vm.createContext(context);
  vm.runInContext(js, context);
  await context.window.salvarMatricula({ preventDefault() {} });
  assert.equal(submitted.length, 1);
  assert.equal(submitted[0].alunoId, 10);
  assert.equal(submitted[0].cursoId, 20);
  assert.deepEqual(Array.from(submitted[0].materiasIds), [7, 9]);
});
