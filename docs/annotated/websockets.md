# WebSockets

The annotated routing module supports WebSocket endpoints through the `@Ws` annotation. WebSocket handlers are defined as methods that return a `WsHandler` instance, keeping all event listeners grouped together.

## Basic Usage

```java
@Endpoints("/api")
class ChatEndpoints {

    @Ws("/chat")
    WsHandler chatSocket() {
        return new WsHandler() {
            @Override
            public void onConnect(WsConnectContext ctx) {
                System.out.println("Connected: " + ctx.sessionId());
            }

            @Override
            public void onMessage(WsMessageContext ctx) {
                ctx.send("Echo: " + ctx.message());
            }

            @Override
            public void onClose(WsCloseContext ctx) {
                System.out.println("Closed: " + ctx.sessionId());
            }

            @Override
            public void onError(WsErrorContext ctx) {
                System.err.println("Error: " + ctx.error());
            }
        };
    }
}
```

## WsHandler Interface

Methods annotated with `@Ws` **must** return a `WsHandler` instance. This is enforced at startup — the plugin will throw an error otherwise.

The `WsHandler` interface provides default (no-op) implementations for all event methods. Override only the ones you need:

```kotlin
interface WsHandler {
    fun onConnect(ctx: WsConnectContext) {}
    fun onError(ctx: WsErrorContext) {}
    fun onClose(ctx: WsCloseContext) {}
    fun onMessage(ctx: WsMessageContext) {}
    fun onBinaryMessage(ctx: WsBinaryMessageContext) {}
}
```

## Available Events

| Method | Context Type | When It Fires |
|--------|-------------|---------------|
| `onConnect` | `WsConnectContext` | Client connects |
| `onMessage` | `WsMessageContext` | Text message received |
| `onBinaryMessage` | `WsBinaryMessageContext` | Binary message received |
| `onClose` | `WsCloseContext` | Connection closed |
| `onError` | `WsErrorContext` | Error occurs |

## Path Parameters

WebSocket paths support parameters just like HTTP routes:

```java
@Ws("/chat/{room}")
WsHandler roomSocket() {
    return new WsHandler() {
        @Override
        public void onConnect(WsConnectContext ctx) {
            String room = ctx.pathParam("room");
            System.out.println("Joined room: " + room);
        }
    };
}
```

## Kotlin Example

```kotlin
@Endpoints("/api")
class ChatEndpoints {

    @Ws("/chat")
    fun chatSocket() = object : WsHandler {
        override fun onConnect(ctx: WsConnectContext) {
            println("Connected: ${ctx.sessionId()}")
        }

        override fun onMessage(ctx: WsMessageContext) {
            ctx.send("Echo: ${ctx.message()}")
        }
    }
}
```

## Path Prefix

The `@Ws` path is combined with the `@Endpoints` prefix, just like HTTP method annotations:

```java
@Endpoints("/api")
class Endpoints {
    @Ws("/events")  // actual path: /api/events
    WsHandler events() { ... }
}
```
