# Testing Guide - toggle-server

Guia objetivo para contribuicoes com IA manterem confiabilidade.

## Piramide de testes

1. Unitarios (maioria)
- Alvo: use cases, mappers e regras puras.
- Ferramentas: JUnit 5 + Mockito.

2. Web slice
- Alvo: contrato HTTP (status, payload, validacao).
- Ferramenta: @WebMvcTest com dependencias mockadas.

3. Integracao (crescimento progressivo)
- Alvo: persistencia, eventos e fluxo de delivery ponta a ponta.
- Recomendacao: SpringBootTest + banco de teste consistente.

## Quando escrever qual teste

- Mudou regra de negocio: unitario obrigatorio.
- Mudou request/response/validacao: @WebMvcTest obrigatorio.
- Mudou repositorio/query/evento: incluir ou ampliar teste de integracao.

## Padrao de cenarios minimos

Para cada mudanca de comportamento, cobrir:
1. Caminho feliz.
2. Entrada invalida.
3. Regra de negocio violada.
4. Erro de dependencia externa (quando aplicavel).

## Convencoes

- Nome de teste descritivo e orientado a comportamento.
- Arrange/Act/Assert claro.
- Um motivo de falha por teste.

## Definition of Done de testes

- Todos os testes locais passando em mvn clean verify.
- Sem teste flaky introduzido.
- Cobertura de novos caminhos criticos adicionada.
