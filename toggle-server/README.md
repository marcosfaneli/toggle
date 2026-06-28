# Toggle Server

Serviço de feature toggles para cadastro de clientes, atualização de toggles e entrega de mudanças por callback.

## Stack
- Java 25
- Spring Boot 3.5
- Spring Data JPA
- Flyway
- MySQL (produção) / H2 (testes)
- Maven

## Requisitos
- JDK 25
- Maven 3.9+

No ambiente local deste workspace:

```bash
export JAVA_HOME=/home/faneli/.jdk/jdk-25.0.2
export PATH=/home/faneli/.jdk/jdk-25.0.2/bin:$PATH
```

## Executar local

```bash
mvn spring-boot:run
```

## Documentacao da API

Com a aplicacao em execucao, acesse:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### Clients

```http
GET /clients
GET /clients?serviceName=checkout-service
```

Lista instancias de clients registradas e suas assinaturas de toggles. O parametro `serviceName` e opcional; quando informado, restringe o resultado ao servico solicitado.

## Build e testes

```bash
mvn clean verify
```

O comando acima executa:
- compilação
- testes
- relatório de cobertura JaCoCo
- gate de cobertura (mínimo inicial de 30% de linhas no bundle)

## Docker

```bash
docker build -t toggle-server:local .
```

## Configuração
As principais variáveis estão em `src/main/resources/application.yml` e incluem:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `toggle.api.max-page-size`
- `toggle.heartbeat.*`
- `toggle.delivery.timeout-ms`

## Banco de dados
As migrações estão em `src/main/resources/db/migration` com versionamento Flyway (`Vxxx__...sql`).
Nunca altere migrações já aplicadas; crie sempre uma nova versão.

## CI
Pipeline em `.github/workflows/build.yml` roda `mvn clean verify` para push e pull request.

## Agentic coding
Documentos principais para contribuicao assistida por IA:
- `AGENTS.md`
- `.github/copilot-instructions.md`
- `.instructions.md`
- `docs/ARCHITECTURE.md`
- `docs/CODE_PATTERNS.md`
- `docs/TESTING.md`
