# Route Comparator

The core module provides a `RouteComparator` that sorts routes in the correct processing order. This ensures that static paths are evaluated before parameterized paths, preventing parameter routes from shadowing specific endpoints.

## How It Works

Routes are sorted using a `RuleBasedCollator` with custom collation rules that prioritize static segments over path parameters:

```
& Z < '{' < '<'
```

This means:
- Static path segments (letters) sort before `{param}` segments
- `{param}` segments sort before `<param>` segments

## Example

Given these routes:

```
/users/{id}
/users/me
/users/{id}/posts
/users
```

The comparator sorts them as:

```
/users
/users/me
/users/{id}
/users/{id}/posts
```

Static paths (`/users`, `/users/me`) come before parameterized paths (`/users/{id}`).

## Usage

The `RouteComparator` is used internally by all routing modules. You don't need to call it directly — routes are automatically sorted when registered.

```kotlin
open class RouteComparator : Comparator<Routed> {
    override fun compare(route: Routed, other: Routed): Int
}
```

It compares `Routed` objects (anything with a `.path` property), not raw strings.

## Path Parameter Syntax

Javalin supports two syntaxes for path parameters:

| Syntax | Example | Description |
|--------|---------|-------------|
| `{param}` | `/users/{id}` | Standard path parameter |
| `<param>` | `/users/<id>` | Slash-accepting path parameter (wildcard) |

Both are handled by the comparator, with `{param}` sorting before `<param>`.
