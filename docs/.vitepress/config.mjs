import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Javalin Routing Extensions',
  description: 'Extensible routing patterns for Javalin',
  base: '/javalin-routing-extensions/',
  appearance: 'dark',
  themeConfig: {
    siteTitle: 'Javalin Routing Extensions',
    nav: [
      { text: 'Documentation', link: '/introduction/setup' },
      {
        text: 'Community',
        items: [
          { text: 'GitHub Issues', link: 'https://github.com/javalin/javalin-routing-extensions/issues' },
          { text: 'Javalin', link: 'https://javalin.io' },
        ],
      },
    ],
    sidebar: [
      {
        text: 'Introduction',
        items: [
          { text: 'Setup', link: '/introduction/setup' },
          { text: 'Overview', link: '/introduction/overview' },
        ],
      },
      {
        text: 'Annotated Routing',
        collapsed: false,
        items: [
          { text: 'Getting Started', link: '/annotated/getting-started' },
          { text: 'HTTP Methods', link: '/annotated/http-methods' },
          { text: 'Parameters', link: '/annotated/parameters' },
          { text: 'Interceptors', link: '/annotated/interceptors' },
          { text: 'Versioning', link: '/annotated/versioning' },
          { text: 'Exception Handling', link: '/annotated/exception-handling' },
          { text: 'WebSockets', link: '/annotated/websockets' },
          { text: 'Lifecycle Events', link: '/annotated/lifecycle-events' },
          { text: 'Result Handlers', link: '/annotated/result-handlers' },
          { text: 'Status Codes', link: '/annotated/status-codes' },
        ],
      },
      {
        text: 'DSL Routing',
        collapsed: false,
        items: [
          { text: 'Getting Started', link: '/dsl/getting-started' },
          { text: 'In-Place DSL', link: '/dsl/in-place' },
          { text: 'Property-Based DSL', link: '/dsl/property-based' },
          { text: 'Type-Safe Paths', link: '/dsl/type-safe-paths' },
          { text: 'Custom DSL', link: '/dsl/custom-dsl' },
        ],
      },
      {
        text: 'Coroutines Routing',
        collapsed: false,
        items: [
          { text: 'Getting Started', link: '/coroutines/getting-started' },
          { text: 'Async vs Sync', link: '/coroutines/async-vs-sync' },
          { text: 'Servlet Configuration', link: '/coroutines/servlet-configuration' },
        ],
      },
      {
        text: 'Advanced',
        collapsed: false,
        items: [
          { text: 'Route Comparator', link: '/advanced/route-comparator' },
          { text: 'Path Clash Detection', link: '/advanced/path-clash-detection' },
        ],
      },
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/javalin/javalin-routing-extensions' },
    ],
    outline: {
      level: [2, 3],
    },
    search: {
      provider: 'local',
    },
    footer: {
      message: 'Released under the Apache 2.0 License.',
      copyright: 'Part of the Javalin ecosystem',
    },
    editLink: {
      pattern: 'https://github.com/javalin/javalin-routing-extensions/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },
  },
})
