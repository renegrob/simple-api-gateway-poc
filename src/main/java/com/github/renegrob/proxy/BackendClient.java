package com.github.renegrob.proxy;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class BackendClient {

    private static final Logger LOG = LoggerFactory.getLogger(BackendClient.class);

    @Inject
    Vertx vertx;

    private WebClient client;

    @PostConstruct
    void initialize() {
        this.client = WebClient.create(vertx,
                new WebClientOptions().setDefaultHost("fruityvice.com").setDefaultPort(443).setSsl(true)
                        .setTrustAll(true).setConnectTimeout(1000).setSslHandshakeTimeout(1000));
    }

    public Uni<JsonObject> getResponse() {
        LOG.info("GET {}", "https://fruityvice.com/api/fruit/apple");
        return client.get("/api/fruit/apple")
                .send()
                .onItem().transform(
                        resp -> {
                            LOG.info("response {}", resp.statusCode());
                            if (resp.statusCode() == 200) {
                                final JsonObject result = resp.bodyAsJsonObject();
                                return result;
                            } else {
                                return new JsonObject()
                                        .put("code", resp.statusCode())
                                        .put("message", resp.bodyAsString());
                            }
                        }
                );
    }

    public void proxy(RoutingContext rc) {
        LOG.info("Original request {}", rc.request().path());
        String forwardPath = rc.request().path().substring("/fruityvice".length());
        LOG.info("{} {}", rc.request().method(), "https://fruityvice.com" + forwardPath);
        final HttpRequest<Buffer> request = client.request(rc.request().method(), forwardPath);
        final MultiMap forwardHeaders = new MultiMap(rc.request().headers());
        // TODO: request body and body size limit
        forwardHeaders.add("X-Forwarded-For", rc.request().remoteAddress().host());
        forwardHeaders.add("X-Forwarded-Proto", rc.request().isSSL() ? "https" : "http");
        // TODO: forwardHeaders.add("X-Forwarded-Port", String.valueOf(rc.));
        LOG.info("headers: {}" , forwardHeaders);
        request.headers().addAll(forwardHeaders);
        request.send().subscribe().with(resp -> {
                    rc.response().headers().addAll(resp.headers().getDelegate());
                    rc.response().setStatusCode(resp.statusCode());
                    LOG.info("response {}", resp.statusCode());
                    rc.response().setStatusMessage(resp.statusMessage());
                    rc.response().end(resp.body().getDelegate());
                });
    }
}
