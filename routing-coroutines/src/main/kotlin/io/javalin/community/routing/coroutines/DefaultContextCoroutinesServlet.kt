package io.javalin.community.routing.coroutines

import io.javalin.http.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DefaultContextCoroutinesServlet<CONTEXT>(
    executorService: ExecutorService = Executors.newCachedThreadPool(),
    private val contextFactory: (Context) -> CONTEXT
) : CoroutinesServlet<CONTEXT, Unit>(
    dispatcher = ExclusiveDispatcherWithShutdown(executorService),
    syncHandler = { ctx, route -> route.handler(contextFactory(ctx)) },
    asyncHandler = { ctx, route, _ -> route.handler(contextFactory(ctx)) },
    errorConsumer = { _, throwable -> throwable.printStackTrace() }
), GracefullyShutdownableDispatcher {

    override fun prepareShutdown() {
        (dispatcher as GracefullyShutdownableDispatcher).prepareShutdown()
    }

    override fun completeShutdown() {
        (dispatcher as GracefullyShutdownableDispatcher).completeShutdown()
    }

}