package com.github.renegrob.proxy.config;

import java.net.URI;
import java.util.List;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties(prefix = "proxy")
public class MappingListConfig {

    public List<MappingConfig> mappings;

    public static class MappingConfig {
        public String path;
        public String backend;
        public String openapi;
        public String schema;
        public String description;

        public String getBackendHost() {
            return backend != null ? URI.create(backend).getHost() : null;
        }
    }
}
