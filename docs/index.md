---
layout: home

hero:
  name: Javalin Routing Extensions
  text: Extensible Routing Patterns
  tagline: Multiple routing approaches for Javalin — annotations, DSL, and coroutines. Choose the pattern that fits your project, from annotation-driven routing to type-safe DSL with path parameters.
  actions:
    - theme: brand
      text: Get Started
      link: /introduction/setup
    - theme: alt
      text: View on GitHub
      link: https://github.com/javalin/javalin-routing-extensions

features:
  - title: Annotated Routing
    details: Annotation-driven routing for Java and Kotlin. Define endpoints with @Get, @Post, and extract parameters with @Body, @Param, @Header, and more.
  - title: DSL Routing
    details: Kotlin DSL with type-safe path parameters, in-place route definitions, and property-based route containers.
  - title: Coroutines Routing
    details: Async and non-blocking endpoint execution using Kotlin coroutines with suspend lambdas and configurable dispatchers.
  - title: API Versioning
    details: Built-in support for versioned endpoints via HTTP headers. Serve multiple API versions from the same codebase with @Version.
  - title: WebSocket Support
    details: Register WebSocket endpoints alongside HTTP routes using @Ws in annotated routing, keeping all WS event listeners grouped together.
  - title: Extensible Architecture
    details: Create custom DSL implementations tailored to your project. The modular design lets you mix and match routing approaches.
---
