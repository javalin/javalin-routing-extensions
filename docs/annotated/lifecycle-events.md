# Lifecycle Events

Register handlers for Javalin server lifecycle events directly in your endpoint classes using `@LifecycleEventHandler`.

## Basic Usage

```java
@Endpoints("/api")
class AppEndpoints {

    @LifecycleEventHandler(lifecycleEvent = JavalinLifecycleEvent.SERVER_STARTED)
    void onServerStarted() {
        System.out.println("Server is ready!");
    }

    @LifecycleEventHandler(lifecycleEvent = JavalinLifecycleEvent.SERVER_STOPPING)
    void onServerStopping() {
        System.out.println("Server is shutting down...");
    }
}
```

## Available Events

| Event | Description |
|-------|-------------|
| `SERVER_STARTING` | Server is starting up |
| `SERVER_STARTED` | Server has started successfully |
| `SERVER_START_FAILED` | Server failed to start |
| `SERVER_STOPPING` | Server is shutting down |
| `SERVER_STOPPED` | Server has stopped |
| `SERVER_STOP_FAILED` | Server failed to stop cleanly |

## Use Cases

### Initialize Resources on Startup

```java
@LifecycleEventHandler(lifecycleEvent = JavalinLifecycleEvent.SERVER_STARTED)
void initializeCache() {
    cacheService.warmUp();
}
```

### Cleanup on Shutdown

```java
@LifecycleEventHandler(lifecycleEvent = JavalinLifecycleEvent.SERVER_STOPPING)
void cleanup() {
    connectionPool.close();
    scheduledTasks.shutdown();
}
```

### Error Reporting

```java
@LifecycleEventHandler(lifecycleEvent = JavalinLifecycleEvent.SERVER_START_FAILED)
void reportStartupFailure() {
    alertService.notify("Server failed to start!");
}
```
