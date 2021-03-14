package com.github.renegrob.proxy;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.renegrob.proxy.config.MappingListConfig;

import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.mutiny.core.Vertx;

import static io.vertx.core.http.impl.HttpUtils.normalizePath;

// https://github.com/EricWittmann/vertx-reverse-proxy/blob/initial-work/src/main/java/io/vertx/ext/reverseproxy/impl/Router.java
@Startup
@ApplicationScoped
public class Proxy {

    @Inject
    Vertx vertx;

    @Inject
    BackendClient backendClient;

    @Inject
    MappingProcessor mappingProcessor;

    private static final Logger LOG = LoggerFactory.getLogger(Proxy.class);

    public void init(@Observes Router router) {
        // TODO: vert.x 4? router.allowForward
        router.route("/fruityvice/*").handler(rc -> backendClient.proxy(rc));
        router.get("/test").handler(rc -> rc.response().end("Hello from my route"));
        router.route("/apple").handler(rc -> {
                    // rc.request().pause();
                    LOG.info("{} {}", rc.request().method(), rc.request().path());
                    final Uni<JsonObject> backendResponse = backendClient.getResponse();
                    backendResponse.subscribe().with(
                            result -> rc.response().end(result.toBuffer())
                    );
                }
        );
        for (MappingListConfig.MappingConfig config : mappingProcessor.getMappings()) {
            LOG.info("Add mapping: " + normalizePath(config.path) + " -> " + config.backend);
            //router.route(config.path).handler(rc -> backendClient.proxy())
        }
        /*
        router.get().failureHandler(ctx -> {
          if (ctx.statusCode() == 404) {
            ctx.reroute("/my-pretty-notfound-handler");
          } else {
            ctx.next();
          }
        });
         */
        router.errorHandler(404, event -> {
            event.response().end("<html><head><title>404</title></head>"
                    + "<body>"
                    + "<h1>404 NOT FOUND: " + escapeHtml(event.request().path()) +"</h1>"
                    + "<p>try <a href=\"/fruityvice/api/fruit/banana\">fruityvice API</a>.</p>"
                    + "</body></html>");
        });
    }

    private static String escapeHtml(final String bodyText) {
        if (bodyText == null) {
            return "";
        }

        return bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
