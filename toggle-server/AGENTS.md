# AGENTS Charter - toggle-server

Este arquivo define como agentes de IA devem colaborar neste repositorio.
Objetivo: reduzir ambiguidade, manter qualidade tecnica e evitar mudancas fora de escopo.

## Escopo

Aplica-se a todo o repositorio.
Complementa as regras em .instructions.md e CONTRIBUTING.md.

## Papeis

### 1) Planner
- Entende o pedido e delimita escopo tecnico.
- Identifica impacto em API, banco, testes e observabilidade.
- Define plano curto de execucao com validacao objetiva.

### 2) Implementer
- Executa mudancas pequenas e focadas.
- Preserva contratos publicos, exceto quando houver solicitacao explicita.
- Inclui ou atualiza testes para toda mudanca de comportamento.
- Nao altera migracoes Flyway existentes em src/main/resources/db/migration.

### 3) Reviewer
- Verifica regressao funcional, quebra de contrato e risco operacional.
- Checa aderencia a arquitetura por contextos (toggle, client, delivery).
- Confirma Definition of Done antes de concluir.

## Handoff entre papeis

1. Planner -> Implementer
- Entrega: objetivo, arquivos alvo, criterios de aceitacao e estrategia de teste.

2. Implementer -> Reviewer
- Entrega: diff objetivo, riscos, comandos de validacao executados e resultado.

3. Reviewer -> Fechamento
- Entrega: parecer final (aprovado ou bloqueado) com pendencias objetivas.

## Regras nao negociaveis

1. Toda mudanca de comportamento exige teste.
2. Nao editar migracao Flyway ja existente.
3. Evitar acoplamento cross-context entre toggle, client e delivery.
4. Manter padrao de erro com ProblemDetail.
5. Executar verificacao de build/testes antes de finalizar.

## Escalonamento para humano

Escalar quando ocorrer qualquer um dos itens abaixo:
- Necessidade de mudar contrato de API (request/response/status).
- Necessidade de alterar modelo de dados existente sem migracao clara.
- Conflito entre regras de arquitetura e pedido funcional.
- Risco de seguranca ou compliance.

## Definition of Done

- Build e testes passando.
- Sem regressao funcional conhecida.
- Documentacao atualizada se houve mudanca de comportamento.
- Mudanca minima necessaria para resolver o pedido.
