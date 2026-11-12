CREATE TABLE usuario (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL,
    papel      VARCHAR(20)  NOT NULL CHECK (papel IN ('CIDADAO', 'ANALISTA', 'GESTOR'))
);

CREATE TABLE alvara (
    id                   BIGSERIAL PRIMARY KEY,
    versao               BIGINT       NOT NULL DEFAULT 0,
    solicitante_id       BIGINT       NOT NULL REFERENCES usuario (id),
    nome_estabelecimento VARCHAR(160),
    atividade            VARCHAR(160),
    endereco             VARCHAR(255),
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    status               VARCHAR(20)  NOT NULL
        CHECK (status IN ('RASCUNHO', 'ENVIADO', 'ANALISADO', 'HOMOLOGADO', 'INDEFERIDO')),
    parecer              VARCHAR(20) CHECK (parecer IN ('FAVORAVEL', 'DESFAVORAVEL')),
    motivo_decisao       VARCHAR(500),
    criado_em            TIMESTAMPTZ  NOT NULL,
    atualizado_em        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_alvara_solicitante ON alvara (solicitante_id);
CREATE INDEX idx_alvara_status ON alvara (status);

CREATE TABLE analise_criterio (
    id         BIGSERIAL PRIMARY KEY,
    alvara_id  BIGINT      NOT NULL REFERENCES alvara (id) ON DELETE CASCADE,
    criterio   VARCHAR(50) NOT NULL,
    atendido   BOOLEAN     NOT NULL,
    observacao VARCHAR(500),
    UNIQUE (alvara_id, criterio)
);

CREATE TABLE documento (
    id           BIGSERIAL PRIMARY KEY,
    alvara_id    BIGINT       NOT NULL REFERENCES alvara (id) ON DELETE CASCADE,
    nome_arquivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    tamanho      BIGINT       NOT NULL,
    conteudo     BYTEA        NOT NULL,
    enviado_em   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_documento_alvara ON documento (alvara_id);

CREATE TABLE unidade (
    id        BIGSERIAL PRIMARY KEY,
    alvara_id BIGINT UNIQUE REFERENCES alvara (id),
    nome      VARCHAR(160)     NOT NULL,
    endereco  VARCHAR(255)     NOT NULL,
    latitude  DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    criada_em TIMESTAMPTZ      NOT NULL
);
