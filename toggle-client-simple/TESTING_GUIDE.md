# Testing Guide

This application works as a feature toggle client. To validate its behavior, follow the steps below.

## 1. Start the backend service
Make sure the toggle server is running and reachable at:

```bash
http://localhost:8080
```

## 2. Start this client
Run the application with:

```bash
mvn spring-boot:run
```

The client will register itself and begin resolving the configured toggles.

## 3. Verify the endpoint
Once the application is up, test the toggle decision endpoint:

```bash
curl http://localhost:8082/internal/toggles/payment-v2/decision
```

You should receive a JSON response showing whether the toggle is enabled and which flow the application will use.

## 4. Expected result
- If the toggle is enabled, the response should indicate the new flow.
- If the toggle is disabled, the response should indicate the legacy flow.

If the response is empty or the client does not register, check the application logs and confirm that the server is available.
