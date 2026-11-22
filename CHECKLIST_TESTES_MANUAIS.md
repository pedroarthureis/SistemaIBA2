# Checklist de testes manuais — Sistema IBA revisado

**Pré-requisito:** execução local em `http://localhost:8181`, usando **somente PostgreSQL de teste**. Nunca insira dados fictícios no banco do Render.

## Fluxo crítico que motivou esta revisão

1. Em **Matérias**, cadastre duas matérias de teste.
2. Em **Cursos**, crie um curso com as caixas de seleção de matérias marcadas; salve.
3. Abra **Notas**, selecione um aluno com matrícula no curso e confira que as disciplinas aparecem na tabela, mesmo sem notas.
4. Teste um curso de teste **sem matérias**: deve aparecer uma mensagem explicativa e o campo de **vincular matéria**. Escolha uma matéria cadastrada e vincule.
5. Confira se a nova disciplina aparece imediatamente na tabela e no seletor do formulário.
6. Clique em **Lançar** no 1º ciclo; confirme que matéria e ciclo estão selecionados corretamente; lance `85,5` ou `85.5`.
7. A linha deve mostrar `85.5` e `PENDENTE` porque ainda faltam os outros ciclos.
8. Clique na nota `85.5` para editar; confira que matéria/ciclo corretos estão selecionados.
9. Lance os outros dois ciclos; verifique a classificação baseada na média mínima de 60.
10. Na seção **Cursos**, clique em editar: a matéria vinculada deve continuar marcada.

## Casos de rejeição

- Tentar inserir nota -1, 100.1, `20abc` ou ciclo inválido: deve recusar.
- Tentar inserir nota para matéria não vinculada (inclusive chamando a API diretamente): deve recusar.
- Tentar remover matéria com notas ou ainda vinculada a curso: deve recusar.
- Tentar excluir aluno com qualquer matrícula: deve recusar.
- Tentar excluir curso com matrículas: deve recusar.
- Tentar remover disciplina de curso com notas: deve recusar.
- Com histórico concluído, tentar alterar as matérias do curso: deve recusar e orientar a criar nova versão curricular.

## Testes de regressão

- Navegar entre todos os módulos, voltar às telas repetidas vezes e usar Voltar/Avançar do navegador.
- Criar/consultar/editar aluno, professor, curso, matéria e matrícula de teste; atualizar a página e conferir persistência.
- Abrir os detalhes da matrícula; ver disciplinas sem notas e com notas, sem tratar nota ausente como zero.
- Testar dashboard, filtros, modal, exportações CSV e doações com dados de teste.
- Abrir DevTools (F12): verificar Console e Network; não deve haver erros relevantes.
- Testar em viewport de celular e no desktop, incluindo navegação por teclado.

## Conferência automática

```powershell
node --test tests/frontend/*.test.cjs
.\mvnw.cmd clean verify
```

**Aprovação de publicação:** além dos comandos acima, executar um teste real de cadastros e notas no **banco de teste** e revisar o código em Pull Request. Esse checklist não substitui testes de segurança e autenticação.


## Novos casos críticos — seleção por aluno (30/10/2026)

1. Em banco de **teste**, crie Curso X com matérias A, B e C.
2. Cadastre Aluno 1 e Aluno 2; em **Matrículas**, escolha A+B para Aluno 1 e B+C para Aluno 2, no mesmo curso/período.
3. Abra **Notas** > Aluno 1: deve ver **somente A e B**. Aluno 2: **somente B e C**. Nunca misturar notas de alunos/períodos diferentes.
4. Lance em Aluno 1 as três notas de A e de B. Deve concluir sem exigir a nota de C; boletim deve omitir C para o Aluno 1.
5. Tente lançar nota em C para Aluno 1 pela API: deve ser rejeitado; o mesmo vale para qualquer matéria não selecionada.
6. Em matrícula **em andamento**, clique **Editar matérias**. Adicione/remova matéria sem nota, salve e recarregue: alteração deve persistir somente para aquele aluno.
7. Tente retirar matéria com ao menos 1 ciclo lançado: deve ser bloqueado. Matérias de matrícula concluída também não podem ser alteradas.
8. Teste a antiga matrícula sem `matricula_materia`: deve continuar mostrando a grade completa do curso; após editar passa a ter grade individual explícita.
9. Tente marcar `APROVADO` manualmente pelo endpoint PATCH: deve ser rejeitado; teste trancar e reabrir matrícula `CURSANDO`.
10. Com `IBA_AUTH_ENABLED=true` em homologação, verifique 401 sem credenciais, acesso mediante usuário/senha e recusa de operações de origem externa; teste formulários após autenticação HTTPS.
11. Confira auditoria de notas, matrículas e doações no log após commit. Não devem constar CPF ou nome de aluno.
12. Teste em Chrome/Firefox, desktop e celular; confira rolagem horizontal das tabelas, foco do teclado e editor do modal.

**Não aprovar deploy** sem `./mvnw clean verify` com BUILD SUCCESS e um ciclo completo em PostgreSQL de teste. `node --test` sozinho não valida o servidor Java.
