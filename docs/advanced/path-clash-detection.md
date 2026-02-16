# Path Clash Detection

The annotated routing module detects potential path conflicts at startup and warns you when two or more interceptors (`@Before`, `@BeforeMatched`, `@After`, `@AfterMatched`) share the same path.

## What Gets Checked

Path clash detection applies to interceptor routes:

- `@Before`
- `@BeforeMatched`
- `@After`
- `@AfterMatched`

HTTP method routes (`@Get`, `@Post`, etc.) are **not** checked because Javalin handles method-based routing natively.

## How Clashes Are Detected

Two paths can clash when:

- They have different parameter names at the same depth (e.g., `/users/{id}` vs `/users/{userId}`)
- A wildcard parameter `<param>` overlaps with specific paths
- Curly braces `{param}` and angle brackets `<param>` are used interchangeably at the same position

## Example Warning

```
[WARN] Potential path clash detected for BEFORE routes:
  Path "/api/*" is registered 2 times
```

## How It Works Internally

The path clash detection uses an internal `canPathsClash` function to determine whether two paths could match the same request. It compares paths segment by segment:

- Two static segments clash only if they are identical (`/users` vs `/users`)
- A parameterized segment (`{id}`) can match any static segment, so `/users/{id}` and `/users/me` are considered potentially clashing
- A wildcard segment (`<param>`) matches anything including slashes, so it clashes with all segments at the same depth

At startup, the plugin scans all interceptor routes and logs warnings for any paths that appear more than once for the same route type:

```
[WARN] Potential path clash detected for BEFORE routes:
  Path "/api/*" is registered 2 times
```
