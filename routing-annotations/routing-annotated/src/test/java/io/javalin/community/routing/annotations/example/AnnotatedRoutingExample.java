package io.javalin.community.routing.annotations.example;

import io.javalin.Javalin;
import io.javalin.community.routing.annotations.*;
import io.javalin.http.Context;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.router.Endpoint;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jetbrains.annotations.Nullable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.javalin.community.routing.annotations.AnnotatedRouting.Annotated;
import static io.javalin.http.Header.AUTHORIZATION;
import static io.javalin.openapi.HttpMethod.GET;

public final class AnnotatedRoutingExample {

    // some dependencies
    static final class ExampleService {
        private final Map<String, ExampleDto> data = new HashMap<>();
        ExampleDto findExampleByName(String name) { return data.get(name); }
        void saveExample(ExampleDto entity) { data.put(entity.name, entity); }
    }

    // some entities
    static final class ExampleDto implements Serializable {
        private String name;

        public ExampleDto() { /* Jackson */ }
        public ExampleDto(String name) { this.name = name; }

        void setName(String name) { this.name = name; }
        public String getName() { return name; }
    }

    // register endpoints with prefix
    @Endpoints("/api")
    static final class ExampleEndpoints {

        private final ExampleService exampleService;

        // pass dependencies required to handle requests
        public ExampleEndpoints(ExampleService exampleService) {
            this.exampleService = exampleService;
        }

        // use Javalin-specific routes
        @Before
        void beforeEach(Context ctx) {
            System.out.println("Before each request: " + ctx.method() + " " + ctx.path());
        }

        @BeforeMatched
        void beforeMatched(Context ctx, Endpoint endpoint) {
            System.out.println("Before matched request to " + endpoint.getMethod() + " " + endpoint.getPath());
        }

        // describe http method and path with annotation
        @Post("/hello")
        // use parameters to extract data from request
        void saveExample(Context context, @Nullable @Header(AUTHORIZATION) String authorization, @Body ExampleDto entity) {
            if (authorization == null) {
                context.status(401);
                return;
            }
            exampleService.saveExample(entity);
        }

        // you can combine it with OpenApi plugin
        @OpenApi(
                path = "/api/hello/{name}",
                methods = { GET },
                summary = "Find example by name",
                pathParams = { @OpenApiParam(name = "name", description = "Name of example to find") },
                responses = { @OpenApiResponse(status = "200", description = "Example found", content = @OpenApiContent(from = ExampleDto.class)) },
                versions = { "default", "2" }
        )
        // you can also use out-of-the-box support for versioned routes
        @Version("2")
        @Get("/hello/{name}")
        void findExampleV2(Context context, @Param String name) {
            context.result(exampleService.findExampleByName(name).name);
        }

        /* OpenApi [...] */
        @Version("1")
        @Get("/hello/{name}")
        void findExampleV1(Context ctx) {
            throw new UnsupportedOperationException("Deprecated");
        }

        // register exception handlers alongside endpoints
        @ExceptionHandler(Exception.class)
        void defaultExceptionHandler(Exception e, Context ctx) {
            ctx.status(500).result("Something went wrong: " + e.getClass());
        }

    }

    public static void main(String[] args) {
        Javalin.createAndStart(config -> {
            // prepare dependencies
            var exampleService = new ExampleService();

            // register endpoints
            config.router.mount(Annotated, routing -> {
                routing.registerEndpoints(new ExampleEndpoints(exampleService));
            });
        });

        // test request to `saveExample` endpoint
        HttpResponse<?> saved = Unirest.post("http://localhost:8080/api/hello")
                .basicAuth("Panda", "passwd")
                .body(new ExampleDto("Panda"))
                .asEmpty();
        System.out.println("Entity saved: " + saved.getStatusText()); // Entity saved: OK

        // test request to `findExampleV2` endpoint
        String result = Unirest.get("http://localhost:8080/api/hello/Panda")
                .header("X-API-Version", "2")
                .asString()
                .getBody();
        System.out.println("Entity: " + result); // Entity: Panda
    }

}