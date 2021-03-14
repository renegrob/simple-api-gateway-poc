package com.github.renegrob.proxy;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.renegrob.proxy.config.MappingListConfig;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class MappingProcessor {

    @Inject
    MappingListConfig mappingListConfig;

    @PostConstruct
    void init() {
    }

    public List<MappingListConfig.MappingConfig> getMappings() {
        return mappingListConfig.mappings;
    }
}
