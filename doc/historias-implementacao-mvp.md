# Historias de Implementacao - MVP Feature Toggle (Incremental)

## 1. Objetivo deste documento

Definir as historias de implementacao do MVP em fatias pequenas.

Regra principal:

- Nao criar todo o banco de uma vez.
- Cada historia cria apenas as tabelas/colunas/indices obrigatorios para seu proprio escopo.
- Cada historia deve entregar algo utilizavel e testavel de ponta a ponta.

---

## 2. Convencoes para todas as historias

- Banco relacional com PK BIGINT e publicId (UUIDv7/ULID) quando aplicavel.
- Migrations versionadas por historia (ex: V001__, V002__, ...).
- Sempre incluir rollback/logica reversivel quando a stack suportar.
- Sempre incluir testes de contrato do endpoint da historia.
- Erros HTTP devem seguir ProblemDetail (RFC 7807 / application/problem+json) em todas as APIs.
- Todo erro deve incluir no minimo os campos padrao do Spring ProblemDetail: `status`, `title`, `detail` e `instance`.
- Para regras de unicidade, preferir `insert` direto com `UNIQUE` no banco e mapear a violacao para erro de negocio (409), evitando pre-consulta (`check-then-insert`).

---

## 3. Backlog de Historias

## H1 - Criar Feature Toggle (POST /toggles) ✅ CONCLUÍDA

Objetivo:

- Permitir cadastro de uma toggle booleana no servidor.

API:

- `POST /toggles`

Banco (somente necessario para H1):

- Criar tabela `feature_toggle` com:
  - `id` BIGINT PK
  - `public_id` VARCHAR(36) UNIQUE
  - `name` VARCHAR
  - `owner_service_name` VARCHAR
  - `enabled` BOOLEAN
  - `version` BIGINT
  - `updated_at` TIMESTAMP
- Criar indice unico em `(name, owner_service_name)`.

Regras de negocio:

- Iniciar `version = 1` no cadastro.
- Nao permitir duplicidade de `name` para o mesmo `owner_service_name`.

Criterios de aceite:

- `POST /toggles` cria registro com sucesso (201).
- Duplicidade retorna erro de negocio (409 ou equivalente).

Fora de escopo nesta historia:

- Valor tipado da toggle.
- Registro de cliente/pod.
- Sincronizacao de entrega.

---

## H2 - Adicionar valor opcional da toggle (objeto filho) ✅ CONCLUÍDA

Objetivo:

- Permitir que a toggle tenha valor opcional tipado (STRING/NUMBER).

API:

- Extensao de `POST /toggles` para aceitar `value` opcional.

Banco (somente necessario para H2):

- Criar tabela `feature_toggle_value` com:
  - `id` BIGINT PK
  - `toggle_id` BIGINT FK -> `feature_toggle.id`
  - `value_type` VARCHAR (STRING|NUMBER)
  - `value_raw` VARCHAR
  - `updated_at` TIMESTAMP
- Criar unique em `toggle_id` (relacao 0..1).

Regras de negocio:

- Se `value` existir: `value.type` obrigatorio e `value.raw` obrigatorio.
- Se `value` nao existir: nao criar linha em `feature_toggle_value`.

Criterios de aceite:

- Cadastro sem `value` continua funcionando.
- Cadastro com `value` persiste filho corretamente.
- Tipo invalido retorna erro de validacao (400).

Fora de escopo nesta historia:

- Atualizacao de toggle.
- Distribuicao para clientes.

---

## H3 - Listar toggles com paginacao (GET /toggles) ✅ CONCLUÍDA

Objetivo:

- Permitir consulta de toggles por servico dono com retorno paginado.

API:

- `GET /toggles?ownerServiceName={ownerServiceName}&enabled={enabled}&page={page}&size={size}&sort={field,asc|desc}`

Banco (somente necessario para H3):

- Nenhuma tabela nova obrigatoria.
- Apenas ajustes de indice se forem estritamente necessarios para performance de listagem por `owner_service_name` e ordenacao.

Regras de negocio:

- Retornar lista de toggles com `value` quando existir.
- Se `enabled` for informado, filtrar por estado (`true|false`).
- Retornar payload paginado com metadados de pagina (`content`, `number`, `size`, `first`, `last`) sem campos de total.
- `page` padrao = 0 e `size` padrao = 20 quando nao informado.
- Header `Link` RFC 8288 com navegacao (`rel="first"`, `rel="prev"`, `rel="next"` quando aplicavel).

Criterios de aceite:

- GET retorna pagina de toggles com sucesso (200).
- Resposta contem `content`, `number` e `size` consistentes com a consulta.
- GET com `enabled=true` retorna apenas toggles habilitadas.
- GET com `enabled=false` retorna apenas toggles desabilitadas.
- Parametros invalidos de paginacao retornam erro de validacao (400).

Fora de escopo nesta historia:

- Filtro por texto livre.

---

## H4 - Atualizar toggle (PATCH /toggles/{name}) ✅ CONCLUÍDA

Objetivo:

- Atualizar `enabled` e/ou `value` de uma toggle existente.

API:

- `PATCH /toggles/{name}`

Banco (somente necessario para H3):

- Nenhuma tabela nova obrigatoria.
- Apenas ajustes de coluna/indice se forem estritamente necessarios para performance de busca por `name` + `owner_service_name`.

Regras de negocio:

- Incrementar `version` a cada alteracao.
- Atualizar `updated_at`.
- Criar/atualizar/remover `feature_toggle_value` conforme payload.

Criterios de aceite:

- PATCH altera estado com sucesso.
- `version` aumenta de forma monotonicamente crescente.

Fora de escopo nesta historia:

- Entrega para clientes/pods.

---

## H5 - Registrar instancia cliente (POST /clients/register)

Objetivo:

- Permitir que cada pod se registre no startup com callback e toggles consumidas.

API:

- `POST /clients/register`
- `DELETE /clients/register/{instanceId}`

Banco (somente necessario para H4):

- Criar tabela `client_instance` com:
  - `id` BIGINT PK
  - `public_id` VARCHAR(36) UNIQUE
  - `service_name` VARCHAR
  - `instance_id` VARCHAR
  - `pod_name` VARCHAR
  - `namespace` VARCHAR
  - `callback_url` VARCHAR
  - `status` VARCHAR
  - `registered_at` TIMESTAMP
  - `last_heartbeat_at` TIMESTAMP NULL
  - `last_seen_at` TIMESTAMP NULL
- Unique em `(service_name, instance_id)`.

- Criar tabela `client_toggle_subscription` com:
  - `id` BIGINT PK
  - `public_id` VARCHAR(36) UNIQUE
  - `client_instance_id` BIGINT FK -> `client_instance.id`
  - `toggle_name` VARCHAR
  - `consume_mode` VARCHAR (LOCAL_CACHE|REMOTE_ALWAYS)
  - `created_at` TIMESTAMP

Regras de negocio:

- Cada toggle em `consumed` deve ter estrategia definida.
- Registro deve retornar snapshot atual das toggles assinadas.

Criterios de aceite:

- Registro por pod funciona.
- Deregister remove/encerra registro da instancia.

Fora de escopo nesta historia:

- Controle de sync por tentativa de entrega.

---

## H6 - Heartbeat da instancia (POST /clients/heartbeat)

Objetivo:

- Manter estado de pod ativo e permitir expiracao de registros stale.

API:

- `POST /clients/heartbeat`

Banco (somente necessario para H5):

- Nenhuma tabela nova obrigatoria.
- Reusar colunas `last_heartbeat_at` e `last_seen_at` de `client_instance`.

Regras de negocio:

- Atualizar heartbeat por `instance_id`.
- Marcar inativo por politica de timeout (job/processo simples).

Criterios de aceite:

- Heartbeat atualiza timestamps.
- Instancias sem heartbeat podem ser expiradas.

Fora de escopo nesta historia:

- Retry de entrega.

---

## H7 - Entrega de atualizacao via PUT para clientes

Objetivo:

- Ao alterar toggle, enviar update para cada instancia consumidora registrada.

API:

- Chamada server -> client: `PUT {callbackUrl}/{toggleName}`

Banco (somente necessario para H6):

- Criar tabela `toggle_sync_state` com:
  - `id` BIGINT PK
  - `public_id` VARCHAR(36) UNIQUE
  - `toggle_id` BIGINT FK -> `feature_toggle.id`
  - `client_instance_id` BIGINT FK -> `client_instance.id`
  - `target_version` BIGINT
  - `last_delivered_version` BIGINT
  - `sync_status` VARCHAR (SYNCED|PENDING_RESPONSE|OUT_OF_SYNC|INACTIVE)
  - `last_delivery_attempt_at` TIMESTAMP NULL
  - `last_response_at` TIMESTAMP NULL
  - `retry_count` INT
  - `last_error` VARCHAR NULL
- Unique em `(toggle_id, client_instance_id)`.

Regras de negocio:

- Marcar `PENDING_RESPONSE` antes do PUT.
- Em HTTP 200: marcar `SYNCED` e atualizar `last_delivered_version`.
- Em timeout/falha: marcar `OUT_OF_SYNC` e incrementar `retry_count`.

Criterios de aceite:

- Alteracao de toggle dispara entrega para todas as instancias assinantes.
- Status por instancia reflete resultado da entrega.

Fora de escopo nesta historia:

- Historico detalhado de cada tentativa.

---

## H8 - Historico de tentativas de entrega

Objetivo:

- Auditar cada tentativa de callback HTTP feita pelo servidor.

Banco (somente necessario para H7):

- Criar tabela `toggle_delivery_event` com:
  - `id` BIGINT PK
  - `public_id` VARCHAR(36) UNIQUE
  - `toggle_id` BIGINT FK -> `feature_toggle.id`
  - `client_instance_id` BIGINT FK -> `client_instance.id`
  - `http_method` VARCHAR
  - `request_url` VARCHAR
  - `request_body_hash` VARCHAR
  - `response_code` INT NULL
  - `response_body` TEXT NULL
  - `attempt` INT
  - `created_at` TIMESTAMP
- Indice em `(client_instance_id, created_at)`.

Regras de negocio:

- Registrar evento em toda tentativa de PUT.

Criterios de aceite:

- Historico consultavel para troubleshooting.

---

## H9 - Runtime da lib-client (LOCAL_CACHE e REMOTE_ALWAYS)

Objetivo:

- Implementar resolucao de toggle no cliente conforme estrategia declarada.

Escopo tecnico:

- Leitura de configuracao `consumed` (mapa toggle -> estrategia).
- LOCAL_CACHE: ler do cache local alimentado por bootstrap/PUT.
- REMOTE_ALWAYS: consultar servidor em tempo real.
- Ignorar update com `version` menor que a atual.

Banco:

- Nenhuma mudanca obrigatoria no servidor para esta historia.

Criterios de aceite:

- Duas estrategias funcionando por toggle.
- Comportamento consistente com controle de versao.

---

## 4. Ordem sugerida de entrega

1. H1 - Criar toggle ✅
2. H2 - Valor opcional ✅
3. H3 - Listar toggles com paginacao ✅
4. H4 - Atualizar toggle
5. H5 - Registro de instancia
6. H6 - Heartbeat
7. H7 - Entrega via PUT + sync state
8. H8 - Historico de entrega
9. H9 - Runtime da lib-client

---

## 5. Definicao de pronto por historia

- Endpoint implementado e testado.
- Migration aplicada somente com estruturas do escopo.
- Contrato de API documentado/atualizado.
- Logs minimos para troubleshooting.
- Sem introduzir tabelas futuras fora da necessidade da historia.
