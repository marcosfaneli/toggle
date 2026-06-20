# Copilot Instructions - toggle-server

Instrucoes operacionais para contribuicoes assistidas por IA neste repositorio.

## Contexto do projeto

- Stack: Java 25, Spring Boot 3.5, Maven, JPA, Flyway.
- Dominios principais: toggle, client, delivery.
- Arquitetura: web -> application -> persistence/domain.

## Prioridades

1. Correcao funcional e preservacao de contrato.
2. Mudancas pequenas e rastreaveis.
3. Testabilidade e legibilidade.
4. Coerencia com regras de contribuicao existentes.

## Guardrails obrigatorios

1. Sempre adicionar/ajustar testes quando houver mudanca de comportamento.
2. Nunca alterar migracoes Flyway existentes em src/main/resources/db/migration.
3. Preservar tratamento de erro baseado em ProblemDetail.
4. Evitar acoplamento cross-context entre toggle, client e delivery.
5. Evitar N+1 em consultas potencialmente custosas.

## Convenios de implementacao

- Preferir constructor injection.
- Evitar refatoracao ampla sem necessidade funcional.
- Manter APIs publicas estaveis salvo pedido explicito.
- Em mudancas assincronas, garantir configuracao explicita de executor quando aplicavel.

## Regras de revisao

Antes de concluir qualquer tarefa:
1. Executar mvn clean verify.
2. Reportar arquivos alterados e riscos residuais.
3. Confirmar que nao houve quebra de contrato de API.
4. Confirmar que regras deste arquivo e de .instructions.md foram atendidas.

## Quando pedir decisao humana

- Mudanca de contrato de API.
- Trade-off de arquitetura entre contextos.
- Mudanca sensivel de seguranca.
- Comportamento ambiguo nao coberto por testes/requisitos.
