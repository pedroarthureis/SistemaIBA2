const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

const src = fs.readFileSync(path.join(__dirname, '../../src/main/resources/static/js/utils.js'), 'utf8');
const context = vm.createContext({ Intl, Date, console });
vm.runInContext(src, context);

// Testes executados sem navegador, servidor ou banco de dados.
test('data ISO nao muda de dia em outro fuso', () => {
  assert.equal(context.formatDate('2026-10-21'), '21/10/2026');
  assert.equal(context.formatDateInput('2026-10-21'), '2026-10-21');
});
test('trata datas vazias e invalidas', () => {
  assert.equal(context.formatDate(null), '');
  assert.equal(context.formatDate('invalida'), '');
});
test('escapa tags, aspas e caracteres especiais', () => {
  assert.equal(context.escapeHtml("<img src=\"x\" onerror='x'>&"), '&lt;img src=&quot;x&quot; onerror=&#39;x&#39;&gt;&amp;');
});
test('CSV escapa virgula, aspas e quebra de linha', () => {
  assert.equal(context.csvCell('Silva, Maria'), '"Silva, Maria"');
  assert.equal(context.csvCell('Fulano "Jr"'), '"Fulano ""Jr"""');
  assert.equal(context.csvCell('linha 1\nlinha 2'), '"linha 1\nlinha 2"');
});
test('CSV neutraliza texto que poderia virar formula', () => {
  assert.equal(context.csvCell('=1+2'), `"'=1+2"`);
  assert.equal(context.csvCell('  @SUM(A1)'), `"'  @SUM(A1)"`);
});
test('media simples e classificacao basica', () => {
  assert.equal(context.calcularMedia([60, 80, 100]), 80);
  assert.equal(context.calcularMedia([]), 0);
  assert.equal(context.getStatusClass('APROVADO'), 'status-success');
});
