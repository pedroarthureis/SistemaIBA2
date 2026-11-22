# Orientações técnicas do Sistema IBA

## Gestão acadêmica

- A grade do curso é definida pelas matérias vinculadas ao curso.
- No cadastro de matrícula, o conjunto de matérias é registrado individualmente.
- O lançamento de notas deve validar que a disciplina pertence à matrícula.
- A exclusão de vínculos com notas existentes é bloqueada para preservar o histórico.
- Boletins e classificação acadêmica devem considerar as disciplinas cursadas.

## Banco de dados

A associação `matricula_materia` é uma alteração aditiva no esquema PostgreSQL.
O arquivo `db/01_matricula_materia.sql` serve como referência para análise
e implantação controlada. Não executar o script no banco de produção sem
avaliar a estrutura existente, realizar backup e testar a restauração.

A configuração atual utiliza `spring.jpa.hibernate.ddl-auto=update`, que
pode criar/alterar tabelas no início da aplicação. Isso exige cautela em
produção. Recomendam-se migrações versionadas e revisão do banco pela equipe.

## Testes

Execute em ambiente separado, usando dados fictícios:

```bash
node --test tests/frontend/*.test.cjs
./mvnw clean verify
```

Os testes manuais a executar estão detalhados em
`CHECKLIST_TESTES_MANUAIS.md`. Priorizar matrícula de alunos diferentes
no mesmo curso, notas por ciclo, alteração de disciplinas, boletins,
rejeição de notas inválidas e persistência após reinicialização.

A aprovação dos testes automatizados não substitui testes ponta a ponta
com PostgreSQL nem avaliação de segurança.

## Controle de acesso

A proteção administrativa HTTP Basic é opcional e está desativada por padrão.
Para ativá-la, configurar `IBA_AUTH_ENABLED=true`, `IBA_ADMIN_USER`,
`IBA_ADMIN_PASSWORD` (mínimo de 12 caracteres) e `IBA_PUBLIC_ORIGIN`
quando o aplicativo estiver atrás de um proxy HTTPS. As credenciais
não devem ser adicionadas ao repositório.

A autenticação administrativa compartilhada não implementa usuários
individuais nem autorização por perfil. Antes de operar com dados reais,
é necessário validar uma solução adequada para controle de acesso,
proteção de dados, backup e rastreabilidade.

## Publicação e revisão

Publicar inicialmente em branch separada, abrir Pull Request e revisar
as alterações com a equipe. Não fazer merge na branch principal antes
de validar os cenários manuais e a alteração do esquema de banco de dados.
