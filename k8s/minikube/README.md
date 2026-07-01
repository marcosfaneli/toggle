# Minikube Validation

This directory runs Switchboard in a local Minikube cluster with:

- `toggle-server`
- `toggle-client-simple` with 2 replicas
- `toggle-admin-ui`

The server uses the MySQL instance already exposed by the project
`docker compose` setup on `localhost:3306`. Inside Minikube, the host is
available through `host.minikube.internal`, so the server `DB_HOST` is:

```text
host.minikube.internal
```

The client registers callbacks using the real IP address of each pod:

```text
http://${POD_IP}:8082/internal/feature-toggles
```

This is intentional. To validate local-cache updates across multiple replicas,
the callback must not use the client `Service`, because the `Service` would load
balance the call and could update only one pod.

## Build Images

From the repository root:

```bash
minikube image build -t toggle-server:minikube ./toggle-server
minikube image build -t toggle-client-simple:minikube ./toggle-client-simple
minikube image build -t toggle-admin-ui:minikube ./toggle-admin-ui
```

Alternative, if your Minikube version does not support `image build`:

```bash
eval "$(minikube docker-env)"
docker build -t toggle-server:minikube ./toggle-server
docker build -t toggle-client-simple:minikube ./toggle-client-simple
docker build -t toggle-admin-ui:minikube ./toggle-admin-ui
```

## Local Database

Before starting the cluster, confirm that the Compose MySQL service is running:

```bash
docker compose up -d mysql
```

The server expects the project default credentials:

| Variable | Value |
| --- | --- |
| `DB_HOST` | `host.minikube.internal` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `toggle_db` |
| `DB_USERNAME` | `toggle` |
| `DB_PASSWORD` | `toggle` |

## Step-By-Step Deploy

Start the namespace and server first:

```bash
kubectl apply -f k8s/minikube/namespace.yaml
kubectl apply -n toggle -f k8s/minikube/server.yaml

kubectl -n toggle rollout status deployment/toggle-server
```

Open a port-forward to the server:

```bash
kubectl -n toggle port-forward svc/toggle-server 18080:8080
```

In another terminal, create the toggles consumed by the client:

```bash
curl -i -X POST http://localhost:18080/toggles \
  -H 'Content-Type: application/json' \
  -d '{"name":"new-checkout","maintainer":"checkout-service","enabled":true,"value":{"type":"STRING","raw":"variant-a"}}'

curl -i -X POST http://localhost:18080/toggles \
  -H 'Content-Type: application/json' \
  -d '{"name":"payment-v2","maintainer":"checkout-service","enabled":true,"value":{"type":"NUMBER","raw":"10"}}'
```

Then start the client:

```bash
kubectl apply -n toggle -f k8s/minikube/client.yaml
kubectl -n toggle rollout status deployment/toggle-client-simple
```

Start the admin UI:

```bash
kubectl apply -n toggle -f k8s/minikube/admin-ui.yaml
kubectl -n toggle rollout status deployment/toggle-admin-ui
kubectl -n toggle port-forward svc/toggle-admin-ui 14200:80
```

Open `http://localhost:14200`.

## Verification

Confirm that two instances are registered:

```bash
curl -s 'http://localhost:18080/clients?serviceName=checkout-service'
```

The response should list 2 `ACTIVE` records, with different `instanceId`,
`podName`, and `callbackUrl` values.

Update the cached toggle:

```bash
curl -i -X PATCH http://localhost:18080/toggles/new-checkout \
  -H 'Content-Type: application/json' \
  -d '{"enabled":false,"value":{"type":"STRING","raw":"variant-b"}}'
```

Verify on the server that delivery was completed for every replica:

```bash
kubectl -n toggle logs deployment/toggle-server | grep 'toggle_delivery'
```

Look for:

```text
subscribersCount=2
event=toggle_delivery_success
```

Verify on the clients that each pod applied the update:

```bash
kubectl -n toggle logs deployment/toggle-client-simple | grep 'Applied callback update'
```

To query a specific replica, list the pods:

```bash
kubectl -n toggle get pods -l app.kubernetes.io/name=toggle-client-simple
```

Then port-forward to each pod, one at a time:

```bash
kubectl -n toggle port-forward pod/<client-pod-name> 18082:8082
curl -s http://localhost:18082/internal/toggles/new-checkout/resolve
```

The expected result for each pod is:

```json
{
  "name": "new-checkout",
  "mode": "LOCAL_CACHE",
  "found": true,
  "enabled": false,
  "version": 2,
  "source": "CACHE"
}
```

## Scale Test

Scale the client and repeat the update:

```bash
kubectl -n toggle scale deployment/toggle-client-simple --replicas=5
kubectl -n toggle rollout status deployment/toggle-client-simple

curl -i -X PATCH http://localhost:18080/toggles/new-checkout \
  -H 'Content-Type: application/json' \
  -d '{"enabled":true,"value":{"type":"STRING","raw":"variant-c"}}'
```

Validate again:

```bash
curl -s 'http://localhost:18080/clients?serviceName=checkout-service'
kubectl -n toggle logs deployment/toggle-server | grep 'toggle_delivery'
kubectl -n toggle logs deployment/toggle-client-simple | grep 'Applied callback update'
```

## Apply Everything At Once

After the toggles already exist, you can also apply everything with Kustomize:

```bash
kubectl apply -k k8s/minikube
```

## Cleanup

```bash
kubectl delete namespace toggle
```
