# Toggle Admin UI

Angular administrative UI for Switchboard toggles.

## Requirements

- Node.js 24+
- npm 11+
- `toggle-server` running on `http://localhost:8080`

## Run Locally

```bash
npm install
npm start
```

The app runs on `http://localhost:4200`.

Local development uses `proxy.conf.json`:

```text
/api -> http://localhost:8080
```

## Build

```bash
npm run build
```

## Test

```bash
npm test -- --watch=false
```

## Docker

```bash
docker build -t toggle-admin-ui:local .
```

The container serves the Angular build with Nginx and proxies `/api` to
`http://toggle-server:8080`, matching Docker Compose and Minikube service names.
