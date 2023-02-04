package io.javalin.community.routing.annotations.example;

import io.javalin.Javalin;
import io.javalin.community.routing.annotations.AnnotationsRoutingPlugin;
import io.javalin.community.routing.annotations.Body;
import io.javalin.community.routing.annotations.Endpoints;
import io.javalin.community.routing.annotations.Get;
import io.javalin.community.routing.annotations.Header;
import io.javalin.community.routing.annotations.Param;
import io.javalin.community.routing.annotations.Post;
import io.javalin.http.Context;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import jakarta.annotation.Nullable;
import kong.unirest.Unirest;
import java.io.Serializable;
import java.util.Objects;

import static io.javalin.http.Header.AUTHORIZATION;
import static io.javalin.openapi.HttpMethod.GET;

public final class AnnotatedRoutingExample {

    // some dependencies
    static final class ExampleService {
        String findExampleByName(String name) { return name; }
        boolean saveExample(ExampleDto entity) { return entity != null; }
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

        // describe http method and path with annotation
        @Post("/hello")
        // use parameters to extract data from request
        void saveExample(Context context, @Nullable @Header(AUTHORIZATION) String authorization, @Body ExampleDto entity) {
            if (authorization == null) {
                context.status(401);
                return;
            }
            context.result(Objects.toString(exampleService.saveExample(entity)));
        }

        // you can combine it with OpenApi plugin
        @OpenApi(
                path = "/hello/{name}",
                methods = { GET },
                summary = "Find example by name",
                pathParams = { @OpenApiParam(name = "name", description = "Name of example to find") },
                responses = { @OpenApiResponse(status = "200", description = "Example found", content = @OpenApiContent(from = ExampleDto.class)) }
        )
        @Get("/hello/{name}")
        void findExample(Context context, @Param String name) {
            context.result(exampleService.findExampleByName(name));
        }

    }

    public static void main(String[] args) {
        Javalin.create(config -> {
            // prepare dependencies
            ExampleEndpoints exampleEndpoints = new ExampleEndpoints(new ExampleService());

            // register endpoints
            AnnotationsRoutingPlugin routingPlugin = new AnnotationsRoutingPlugin();
            routingPlugin.registerEndpoints(exampleEndpoints);
            config.plugins.register(routingPlugin);
        }).start(7000);

        // test request to `saveExample` endpoint
        Boolean saved = Unirest.post("http://localhost:7000/api/hello")
                .basicAuth("Panda", "passwd")
                .body(new ExampleDto("Panda"))
                .asObject(Boolean.class)
                .getBody();
        System.out.println("Entity saved: " + saved);
    }

}