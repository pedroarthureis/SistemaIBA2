-- Referência para implantação controlada no PostgreSQL de TESTE primeiro.
-- Nunca execute diretamente no banco de produção sem backup e aprovação da equipe.
-- Esta tabela é aditiva: não modifica registros de aluno, curso, matrícula ou nota.
-- Hibernate ddl-auto=update também pode criá-la automaticamente no startup;
-- em produção prefira DBA / migração versionada e não faça as duas opções sem verificar.
BEGIN;
CREATE TABLE IF NOT EXISTS matricula_materia (
    matricula_id BIGINT NOT NULL REFERENCES matricula(id),
    materia_id   BIGINT NOT NULL REFERENCES materia(id),
    CONSTRAINT uk_matricula_materia UNIQUE (matricula_id, materia_id)
);
CREATE INDEX IF NOT EXISTS idx_mat_materia_materia ON matricula_materia (materia_id);
COMMIT;
