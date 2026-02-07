package io.javalin.community.routing;

import io.javalin.config.JavalinConfig;
import io.javalin.config.JavalinState;

public class JavalinConfigUnsafe {

    public static JavalinState getState(JavalinConfig config) {
        return config.getState$javalin();
    }

}
