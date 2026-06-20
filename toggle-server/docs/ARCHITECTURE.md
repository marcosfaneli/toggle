# Architecture - toggle-server

## Objetivo

O toggle-server gerencia definicao de toggles, registro de clientes e entrega de atualizacoes por callback.

## Contextos do dominio

### 1) toggle
Responsavel por:
- Criacao, leitura, listagem e atualizacao de toggles.
- Validacao de tipo e valor de toggle.

Pacotes relevantes:
- src/main/java/com/toggle/server/toggle/web
- src/main/java/com/toggle/server/toggle/application
- src/main/java/com/toggle/server/toggle/persistence

### 2) client
Responsavel por:
- Registro e remocao de instancia de cliente.
- Heartbeat e ciclo de vida de instancia.
- Assinaturas de toggles por cliente.

Pacotes relevantes:
- src/main/java/com/toggle/server/client/web
- src/main/java/com/toggle/server/client/application
- src/main/java/com/toggle/server/client/persistence

### 3) delivery
Responsavel por:
- Reacao a atualizacao de toggle.
- Entrega de evento para callbacks de clientes assinantes.
- Registro de estado de sincronizacao de entrega.

Pacotes relevantes:
- src/main/java/com/toggle/server/delivery/application
- src/main/java/com/toggle/server/delivery/infrastructure
- src/main/java/com/toggle/server/delivery/domain

### 4) shared
Responsavel por:
- Utilitarios e tipos compartilhados de infraestrutura/erro/tempo.

Pacotes relevantes:
- src/main/java/com/toggle/server/shared

## Fluxo principal

1. Cliente chama endpoint HTTP no layer web.
2. Controller mapeia command/request e delega para use case no layer application.
3. Use case aplica regra de negocio e persiste/consulta via adapters de persistence.
4. Em atualizacao de toggle, um evento de dominio dispara fluxo de delivery.
5. Delivery tenta notificar callbacks inscritos e atualiza estado de sincronizacao.

## Fronteiras e dependencias

- Regra: web depende de application.
- Regra: application depende de contratos/domain e adapters necessarios.
- Regra: persistence/infrastructure nao define regra de negocio.
- Regra: evitar acoplamento direto entre contextos sem contrato explicito.

## Dados e migracoes

- Banco gerenciado com Flyway em src/main/resources/db/migration.
- Migracoes existentes sao imutaveis.
- Evolucoes de schema devem ser feitas em novas versoes Vxxx__*.sql.

## Erros e contratos HTTP

- Erros HTTP devem seguir ProblemDetail.
- Validacoes de entrada ficam no layer web (Bean Validation) e mapping coerente.

## Testes

- Mudanca de comportamento deve incluir teste.
- Testes web: @WebMvcTest para contrato de endpoint.
- Regras de negocio: testes unitarios com JUnit 5 e Mockito.
- Mudancas em persistencia/eventos devem priorizar ampliacao de cobertura de integracao.

## Decisoes nao negociaveis

1. Nao alterar migracoes Flyway existentes.
2. Nao introduzir acoplamento cross-context desnecessario.
3. Nao quebrar contratos publicos sem alinhamento explicito.
4. Nao concluir tarefa sem validacao de build/teste.
