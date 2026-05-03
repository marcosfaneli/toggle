# Especificacao Tecnica - Plataforma de Feature Toggle (MVP)

## 1. Visao Geral

Esta especificacao define o MVP de uma plataforma de Feature Toggle com:

- Servico Servidor central para cadastro e distribuicao de toggles.
- Biblioteca cliente (Spring) para consumo de toggles.
- Sincronizacao ativa via callback HTTP PUT do servidor para cada pod registrado.
- Estrategia de consumo definida obrigatoriamente pelo cliente (LOCAL_CACHE ou REMOTE_ALWAYS).

Documentos de apoio:

- Fluxo: [feature-toggle-flow.puml](feature-toggle-flow.puml)
- Modelo de dados: [feature-toggle-data-model.puml](feature-toggle-data-model.puml)

---

## 2. Objetivo do MVP

Entregar um mecanismo simples, performatico e escalavel para habilitar/desabilitar funcionalidades e, opcionalmente, fornecer valor tipado simples por toggle.

### 2.1 Requisitos principais

- Cadastro centralizado de toggles no servidor.
- Registro de cada instancia cliente (pod) no startup.
- Distribuicao de atualizacoes para todas as instancias consumidoras.
- Controle de sincronizacao por instancia.
- Suporte a valor opcional com tipos simples: STRING e NUMBER.

### 2.2 Decisoes de arquitetura do MVP

- Estrategia de consumo e responsabilidade do cliente.
- Servidor nao governa estrategia; apenas entrega valores e registra estado de entrega.
- Confirmacao de entrega via resposta HTTP padrao do PUT (200 OK esperado).

### 2.3 Premissa de execucao (PoC)

- Este projeto sera tratado como prova de conceito (PoC).
- Requisitos de seguranca nao serao implementados nesta fase.

---

## 3. Escopo e Fora de Escopo

### 3.1 Escopo MVP

- Toggle booleana com valor opcional.
- Valor opcional em objeto filho, com type + raw.
- Registro por instancia (pod) com callbackUrl proprio.
- Retry de entrega quando PUT falhar por timeout/erro.
- Sincronizacao por instancia com status de entrega.

### 3.2 Fora de escopo neste MVP

- Segmentacao de toggle por usuario/regiao/dispositivo.
- Regras de rollout percentual.
- Governanca central de estrategia.
- Tipos complexos de valor (JSON/ARRAY/OBJECT).
- Seguranca (autenticacao, autorizacao, validacao de origem, hardening).
- UI administrativa completa.

---

## 4. Arquitetura e Topologia

### 4.1 Componentes

- Feature Toggle Server:
  - CRUD de toggles.
  - Registro de instancias consumidoras.
  - Distribuicao de atualizacoes via PUT para callbackUrl de cada pod.
  - Controle de status de sincronizacao por instancia.

- Feature Toggle Client Library (Spring):
  - Leitura de configuracao local no startup.
  - Registro de instancia no servidor.
  - Armazenamento local (cache) quando estrategia LOCAL_CACHE.
  - Consulta remota quando estrategia REMOTE_ALWAYS.
  - Endpoint interno para receber PUT de atualizacao.

- Banco relacional:
  - Persistencia de toggles, instancias, assinaturas, estado de sync e historico de entrega.

### 4.2 Topologia Kubernetes

- Cada pod registra sua propria instancia no servidor.
- O servidor trata entrega por instancia (nao por Service).
- Cada pod expoe endpoint interno de callback.

---

## 5. Implementacao do Servidor

### 5.1 Responsabilidades do Servidor

- Gerenciar ciclo de vida de toggles.
- Persistir e versionar alteracoes de toggles.
- Receber registro/deregister de instancias.
- Distribuir atualizacoes para instancias consumidoras via PUT.
- Manter status de sincronizacao por instancia.

### 5.2 Modelo de Dados (Servidor)

Baseado em [feature-toggle-data-model.puml](feature-toggle-data-model.puml).

Entidades principais:

- FeatureToggle
  - BIGINT (PK), publicId (UUIDv7/ULID), name, ownerServiceName, enabled, version, updatedAt.

- FeatureToggleValue (0..1 por toggle)
  - Objeto filho opcional para valor tipado.
  - toggleId (FK + UNIQUE), valueType (STRING|NUMBER), valueRaw.

- ClientInstance
  - Uma linha por pod ativo.
  - instanceId unico por serviceName.
  - callbackUrl por instancia.

- ClientToggleSubscription
  - Relaciona instancia e toggle consumida.
  - consumeMode enviado pelo cliente.

- ToggleSyncState
  - Estado por toggle + instancia.
  - SYNCED, PENDING_RESPONSE, OUT_OF_SYNC, INACTIVE.

- ToggleDeliveryEvent
  - Historico de tentativas de callback HTTP.

Padrao de IDs:

- PK/FK internas: BIGINT.
- IDs publicos: UUIDv7/ULID.

### 5.3 APIs do Servidor

#### 5.3.0 Contrato de erro padrao

- Todas as respostas de erro do servidor devem usar `application/problem+json` (ProblemDetail).
- Estrutura minima obrigatoria em qualquer endpoint:
  - `status`: codigo HTTP numerico.
  - `title`: resumo estavel do tipo de erro.
  - `detail`: descricao do problema.
  - `instance`: URI da requisicao que falhou.
- Para regras de unicidade, o padrao de implementacao recomendado e `insert` direto protegido por constraint `UNIQUE`, com mapeamento de violacao de integridade para erro de negocio (409).
- Evitar padrao `check-then-insert` para reduzir janela de condicao de corrida em cenarios concorrentes.

Exemplo de erro de negocio (409):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Toggle 'novo-checkout' already exists for service 'checkout-service'",
  "instance": "/toggles"
}
```

#### 5.3.1 Cadastrar toggle

- Metodo: POST
- Rota: /toggles

Request:

```json
{
  "name": "novo-checkout",
  "ownerServiceName": "checkout-service",
  "enabled": true,
  "value": {
    "type": "STRING",
    "raw": "v2"
  }
}
```

Notas:

- value e opcional.
- Quando value existir, type e obrigatorio e deve ser STRING ou NUMBER.

Response 201:

```json
{
  "id": "01J...",
  "name": "novo-checkout",
  "ownerServiceName": "checkout-service",
  "enabled": true,
  "value": {
    "type": "STRING",
    "raw": "v2"
  },
  "version": 1,
  "updatedAt": "2026-04-21T10:00:00Z"
}
```

#### 5.3.2 Atualizar toggle

- Metodo: PATCH
- Rota: /toggles/{name}

Request:

```json
{
  "enabled": false,
  "value": {
    "type": "NUMBER",
    "raw": "10"
  }
}
```

Regras:

- Incrementar version a cada alteracao.
- Disparar processo de distribuicao para instancias consumidoras registradas.

#### 5.3.3 Consultar toggles para um servico

- Metodo: GET
- Rota: /toggles?serviceName={service}

Response 200:

```json
{
  "toggles": [
    {
      "name": "novo-checkout",
      "enabled": false,
      "value": {
        "type": "NUMBER",
        "raw": "10"
      },
      "version": 2,
      "updatedAt": "2026-04-21T10:10:00Z"
    }
  ]
}
```

#### 5.3.4 Registrar instancia cliente

- Metodo: POST
- Rota: /clients/register

Request:

```json
{
  "serviceName": "checkout-service",
  "instanceId": "checkout-7d8d4c7f6f-abcde",
  "podName": "checkout-7d8d4c7f6f-abcde",
  "namespace": "payments",
  "callbackUrl": "http://10.42.1.25:8080/internal/feature-toggles",
  "subscriptions": [
    { "toggleName": "novo-checkout", "consumeMode": "LOCAL_CACHE" },
    { "toggleName": "pagamento-v2", "consumeMode": "REMOTE_ALWAYS" }
  ]
}
```

Response 200:

```json
{
  "serviceName": "checkout-service",
  "instanceId": "checkout-7d8d4c7f6f-abcde",
  "toggles": [
    {
      "name": "novo-checkout",
      "enabled": true,
      "value": { "type": "STRING", "raw": "v2" },
      "version": 3,
      "updatedAt": "2026-04-21T10:20:00Z"
    }
  ]
}
```

#### 5.3.5 Remover registro de instancia

- Metodo: DELETE
- Rota: /clients/register/{instanceId}

Response 204.

#### 5.3.6 Heartbeat (recomendado)

- Metodo: POST
- Rota: /clients/heartbeat

Request:

```json
{
  "instanceId": "checkout-7d8d4c7f6f-abcde",
  "timestamp": "2026-04-21T10:30:00Z"
}
```

Uso:

- Atualiza lastHeartbeatAt e lastSeenAt.
- Evita manter registros de pods mortos.

### 5.4 Fluxos do Servidor

#### 5.4.1 Alteracao de toggle

1. Persistir novo estado/value e incrementar version.
2. Identificar instancias consumidoras registradas.
3. Para cada instancia, enviar PUT no callbackUrl.
4. Se response 200, marcar SYNCED.
5. Se timeout/falha, marcar OUT_OF_SYNC e agendar retry.

### 5.5 Retry e Timeout (Servidor)

Sugestao inicial:

- Timeout de callback PUT: 2s.
- Retry com exponential backoff: 1s, 2s, 4s, 8s, 16s.
- Maximo de tentativas: 5.
- Apos maximo, manter OUT_OF_SYNC e registrar erro.

### 5.6 Observabilidade (Servidor)

Logs estruturados (campos minimos):

- toggleName
- instanceId
- version
- callbackUrl
- responseCode
- latencyMs
- syncStatus

Metricas:

- toggle_put_delivery_success_total
- toggle_put_delivery_failure_total
- toggle_put_delivery_latency_ms
- toggle_sync_out_of_sync_instances
- toggle_register_total
- toggle_register_active_instances

Tracing:

- Propagar correlationId/requestId do servidor para callback PUT.

### 5.7 Performance e Escalabilidade (Servidor)

- PK/FK numericas (BIGINT) para reduzir custo de join/indice.
- Operacoes de leitura devem usar `@Transactional(readOnly = true)`:
  - O Hibernate nao tira snapshot das entidades carregadas (menos memoria).
  - O dirty checking e desativado — sem comparacao estado-atual x snapshot no flush.
  - Nao ha flush antes de queries, reduzindo roundtrips ao banco.
  - Alguns datasources/drivers roteiam transacoes read-only para replicas de leitura automaticamente.
- Indices recomendados:
  - FeatureToggle(name, ownerServiceName) UNIQUE
  - ClientInstance(serviceName, instanceId) UNIQUE
  - ToggleSyncState(toggleId, clientInstanceId) UNIQUE
  - ToggleSyncState(syncStatus)
  - ToggleDeliveryEvent(clientInstanceId, createdAt)
- Distribuicao assincrona por fila interna de entregas (worker) no servidor.

---

## 6. Implementacao da Lib-Client (Spring)

### 6.1 Responsabilidades da Lib-Client

- Ler configuracao local da aplicacao.
- Declarar estrategias por toggle consumida.
- Registrar instancia no startup.
- Expor endpoint interno para callback PUT.
- Avaliar toggle em runtime com LOCAL_CACHE ou REMOTE_ALWAYS.
- Atualizar cache local quando receber atualizacao.

### 6.2 Configuracao do Cliente

Exemplo de propriedades:

```yaml
feature:
  toggles:
    consumed:
      novo-checkout: LOCAL_CACHE
      pagamento-v2: REMOTE_ALWAYS
    callback-url: /internal/feature-toggles
```

Regras:

- Toda toggle consumida deve ter estrategia declarada no mapa consumed.
- callback-url representa o path do endpoint interno da aplicacao.
- No registro, a lib deve enviar callbackUrl completo resolvido para a instancia/pod.

### 6.3 Endpoint de Callback no Cliente

#### 6.3.1 Atualizacao recebida do servidor

- Metodo: PUT
- Rota: /internal/feature-toggles/{toggleName}

Request:

```json
{
  "enabled": false,
  "value": {
    "type": "NUMBER",
    "raw": "10"
  },
  "version": 4,
  "updatedAt": "2026-04-21T10:35:00Z",
  "instanceId": "checkout-7d8d4c7f6f-abcde"
}
```

Response esperada:

- 200 OK em sucesso.
- 4xx/5xx em falha de processamento.

### 6.4 Regras da Lib-Client

- Estrategia e obrigatoria por toggle consumida.
- Servidor nao altera estrategia enviada pelo cliente no MVP.
- value e opcional.
- Se value existir: type obrigatorio (STRING ou NUMBER) e raw obrigatorio.
- Se value nao existir: toggle booleana pura.
- Cliente deve ignorar update com version menor que a atual em cache.

### 6.5 Fluxos da Lib-Client

#### 6.5.1 Startup de pod

1. Lib le configuracao local.
2. Lib registra instancia no servidor.
3. Servidor retorna snapshot atual.
4. Lib inicializa cache local.

#### 6.5.2 Avaliacao de toggle

- LOCAL_CACHE:
  - Consulta cache local.

- REMOTE_ALWAYS:
  - Consulta GET no servidor em tempo real.

#### 6.5.3 Encerramento de pod

- Cliente chama DELETE /clients/register/{instanceId}.
- Se nao ocorrer, servidor expira registro por timeout de heartbeat.

### 6.6 Observabilidade da Lib-Client

- Registrar logs de aplicacao de update (toggleName, version, resultado).
- Expor metricas de cache hit/miss para LOCAL_CACHE.

---

## 7. Regras Compartilhadas (Server + Lib-Client)

### 7.1 Consistencia de Versao

- Toda alteracao de toggle incrementa version monotonicamente.
- Servidor envia version em bootstrap e em callbacks.
- Cliente processa apenas versoes maiores que a local.

### 7.2 Contrato de Valor Opcional

- value e opcional como objeto filho.
- Quando presente: value.type em STRING|NUMBER e value.raw obrigatorio.
- Quando ausente: comportamento booleana da toggle.

---

## 8. Plano de Implementacao (MVP)

### 8.1 Trilhas do Servidor

1. Banco e migracoes
- Criar tabelas e indices principais.

2. API de toggles e clientes
- Implementar endpoints de toggle, registro, heartbeat e deregister.

3. Pipeline de distribuicao
- Implementar worker de entrega PUT com retry.

4. Observabilidade
- Logs, metricas e tracing.

### 8.2 Trilhas da Lib-Client

1. Configuracao Spring
- Ler consumed e callback-url.

2. Registro de instancia
- Montar callbackUrl completo e registrar no startup.

3. Runtime de avaliacao
- Implementar LOCAL_CACHE e REMOTE_ALWAYS.

4. Callback interno
- Expor endpoint PUT e atualizar cache local com controle de versao.

### 8.3 Integracao em Kubernetes

- Configurar conectividade server -> pod.
- Configurar startup/shutdown hooks para register/deregister.
- Validar expiracao por heartbeat para pods mortos.

---

## 9. Criterios de Aceite

- Registro de pods funcionando por instancia.
- Snapshot recebido no startup.
- PUT de atualizacao entregue para todas as instancias registradas.
- Sincronizacao por instancia visivel (SYNCED/OUT_OF_SYNC).
- Estrategias LOCAL_CACHE e REMOTE_ALWAYS funcionando por toggle.
- Valor opcional tipado (STRING/NUMBER) funcionando em bootstrap e update.

---

## 10. Riscos e Mitigacoes

- Risco: callbackUrl inacessivel em Kubernetes.
  - Mitigacao: padronizar descoberta/endereco por pod e validar no registro.

- Risco: registros stale de pods mortos.
  - Mitigacao: heartbeat + expiracao por timeout.

- Risco: overwrite com versao antiga.
  - Mitigacao: validacao de version no cliente.

- Risco: alta taxa de atualizacao gerar fan-out pesado.
  - Mitigacao: worker pool e controle de concorrencia.

---

## 11. Evolucoes futuras (pos-MVP)

- Governanca de estrategia no servidor (override por politica).
- Segmentacao de rollout.
- Tipos de valor adicionais (BOOLEAN/JSON).
- Mensageria para distribuicao em grande escala.
- Painel administrativo de operacao.
