# Minikube validation

Este diretório sobe o Switchboard em um cluster local Minikube com:

- `toggle-server`
- `toggle-client-simple` com 2 réplicas

O server usa o MySQL já publicado pelo `docker compose` do projeto em `localhost:3306`. Dentro do Minikube, o host é acessado por `host.minikube.internal`, então o `DB_HOST` do server fica assim:

```text
host.minikube.internal
```

O client registra o callback usando o IP real de cada pod:

```text
http://${POD_IP}:8082/internal/feature-toggles
```

Isso é intencional. Para validar atualização de cache local em múltiplas réplicas, o callback não deve usar o `Service` do client, porque o `Service` balancearia a chamada e poderia atualizar só um pod.

## Build das imagens

Na raiz do repositório:

```bash
minikube image build -t toggle-server:minikube ./toggle-server
minikube image build -t toggle-client-simple:minikube ./toggle-client-simple
```

Alternativa, se sua versão do Minikube não tiver `image build`:

```bash
eval "$(minikube docker-env)"
docker build -t toggle-server:minikube ./toggle-server
docker build -t toggle-client-simple:minikube ./toggle-client-simple
```

## Banco local

Antes de subir o cluster, confirme que o MySQL do Compose está rodando:

```bash
docker compose up -d mysql
```

O server espera as credenciais padrão do projeto:

| Variável | Valor |
| --- | --- |
| `DB_HOST` | `host.minikube.internal` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `toggle_db` |
| `DB_USERNAME` | `toggle` |
| `DB_PASSWORD` | `toggle` |

## Deploy em etapas

Suba namespace e server primeiro:

```bash
kubectl apply -f k8s/minikube/namespace.yaml
kubectl apply -n toggle -f k8s/minikube/server.yaml

kubectl -n toggle rollout status deployment/toggle-server
```

Abra um port-forward para o server:

```bash
kubectl -n toggle port-forward svc/toggle-server 18080:8080
```

Em outro terminal, crie os toggles consumidos pelo client:

```bash
curl -i -X POST http://localhost:18080/toggles \
  -H 'Content-Type: application/json' \
  -d '{"name":"novo-checkout","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"STRING","raw":"variant-a"}}'

curl -i -X POST http://localhost:18080/toggles \
  -H 'Content-Type: application/json' \
  -d '{"name":"pagamento-v2","ownerServiceName":"checkout-service","enabled":true,"value":{"type":"NUMBER","raw":"10"}}'
```

Agora suba o client:

```bash
kubectl apply -n toggle -f k8s/minikube/client.yaml
kubectl -n toggle rollout status deployment/toggle-client-simple
```

## Verificações

Confirme que há duas instâncias registradas:

```bash
curl -s 'http://localhost:18080/clients?serviceName=checkout-service'
```

O retorno deve listar 2 registros `ACTIVE`, com `instanceId`, `podName` e `callbackUrl` diferentes.

Atualize o toggle cacheado:

```bash
curl -i -X PATCH http://localhost:18080/toggles/novo-checkout \
  -H 'Content-Type: application/json' \
  -d '{"enabled":false,"value":{"type":"STRING","raw":"variant-b"}}'
```

Verifique no server que a entrega foi feita para todas as réplicas:

```bash
kubectl -n toggle logs deployment/toggle-server | grep 'toggle_delivery'
```

Procure por:

```text
subscribersCount=2
event=toggle_delivery_success
```

Verifique nos clients que cada pod aplicou a atualização:

```bash
kubectl -n toggle logs deployment/toggle-client-simple | grep 'Applied callback update'
```

Para consultar uma réplica específica, liste os pods:

```bash
kubectl -n toggle get pods -l app.kubernetes.io/name=toggle-client-simple
```

Depois faça port-forward para cada pod, um por vez:

```bash
kubectl -n toggle port-forward pod/<client-pod-name> 18082:8082
curl -s http://localhost:18082/internal/toggles/novo-checkout/resolve
```

O resultado esperado em cada pod é:

```json
{
  "name": "novo-checkout",
  "mode": "LOCAL_CACHE",
  "found": true,
  "enabled": false,
  "version": 2,
  "source": "CACHE"
}
```

## Teste de escala

Escale o client e repita a atualização:

```bash
kubectl -n toggle scale deployment/toggle-client-simple --replicas=5
kubectl -n toggle rollout status deployment/toggle-client-simple

curl -i -X PATCH http://localhost:18080/toggles/novo-checkout \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true,"value":{"type":"STRING","raw":"variant-c"}}'
```

Valide novamente:

```bash
curl -s 'http://localhost:18080/clients?serviceName=checkout-service'
kubectl -n toggle logs deployment/toggle-server | grep 'toggle_delivery'
kubectl -n toggle logs deployment/toggle-client-simple | grep 'Applied callback update'
```

## Aplicar tudo de uma vez

Depois que os toggles já existem, também é possível aplicar tudo via Kustomize:

```bash
kubectl apply -k k8s/minikube
```

## Limpeza

```bash
kubectl delete namespace toggle
```
