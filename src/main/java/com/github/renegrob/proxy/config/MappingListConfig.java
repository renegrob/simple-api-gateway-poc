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
        private String description;

        public String getDescription() {
            return description != null ? description : URI.create(backend).getHost();
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
