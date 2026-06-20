# Contributing

## Regras de contribuição
- Toda mudança deve incluir ou atualizar testes.
- Use `mvn clean verify` antes de abrir PR.
- Não altere migrações Flyway existentes; adicione nova versão.
- Mantenha validações de entrada no layer web (Bean Validation).
- Preserve tratamento de erro padronizado em `ProblemDetail`.

## Padrões arquiteturais
- Domínio separado por contextos: `toggle`, `client`, `delivery`.
- Fluxo esperado: web -> application -> persistence/domain.
- Evite acoplamento direto entre contextos; prefira contratos explícitos quando evoluir o código.

## Checklist de Pull Request
- [ ] Build e testes locais passaram (`mvn clean verify`)
- [ ] Casos de erro cobertos por teste
- [ ] Sem quebra de contrato de API
- [ ] Migração de banco incluída quando necessário
- [ ] Logs relevantes mantidos para troubleshooting
