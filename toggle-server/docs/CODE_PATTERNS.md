# Code Patterns - toggle-server

Este documento define padroes de implementacao para reduzir variacao em contribuicoes assistidas por IA.

## 1) Novo endpoint HTTP

Checklist:
1. Criar/ajustar DTO de entrada e saida no pacote web do contexto.
2. Aplicar Bean Validation no request.
3. Mapear request para command de application.
4. Delegar regra de negocio para UseCase.
5. Retornar status e payload consistentes com contrato existente.
6. Cobrir casos de sucesso e erro com @WebMvcTest.

Estrutura esperada:
- web: Controller + CommandMapper
- application: UseCase + Command/Result
- domain/persistence: apenas o necessario

## 2) Novo UseCase

Checklist:
1. Classe no pacote application do contexto.
2. Nome orientado a acao (ex.: CreateXUseCase, UpdateYUseCase).
3. Construtor explicito com dependencias.
4. Validacoes de regra de negocio no proprio use case.
5. Sem logica HTTP no application layer.
6. Teste unitario para caminho feliz e regras de erro.

Template simplificado:

```java
@Service
public class ExampleUseCase {

    private final ExampleRepository repository;

    public ExampleUseCase(ExampleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ExampleResult execute(ExampleCommand command) {
        // regra de negocio
        return repository.save(command);
    }
}
```

## 3) Novo adapter de persistencia

Checklist:
1. Manter responsabilidades de I/O e mapeamento.
2. Evitar regra de negocio dentro do adapter.
3. Prevenir N+1 em consultas com colecoes.
4. Garantir queries index-friendly quando possivel.
5. Cobrir comportamento critico com testes de integracao quando aplicavel.

## 4) Novo evento de dominio / entrega

Checklist:
1. Evento deve representar fato de negocio claro.
2. Listener assincrono precisa de tratamento de falha observavel.
3. Nao bloquear thread de request por entrega externa.
4. Persistir estado minimo de sincronizacao para troubleshooting.
5. Garantir teste de regressao para fluxo de entrega.

## 5) Regra de mudanca minima

- Nao misturar refatoracao ampla com mudanca funcional.
- Evitar alterar mais de um contexto sem justificativa explicita.
- Em caso de duvida, quebrar em PRs menores.
