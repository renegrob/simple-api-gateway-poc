package com.github.renegrob.proxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.renegrob.proxy.config.MappingListConfig;

import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiParser;

import static io.vertx.core.http.impl.HttpUtils.normalizePath;

public class OpenApiFilter implements OASFilter {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiFilter.class);
    public static final String DEFAULT_TAG = "default";

    public static final Pattern URL_SCHEME = Pattern.compile("^([a-z][a-z0-9+\\-.]*):");


    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        //String actual = OpenApiSerializer.serialize(OpenApiDocument.INSTANCE.get(), Format.JSON);
        //OpenApiParser.parse()

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }

        final Info info = OASFactory.createInfo();
        info.setTitle("Proxy");
        info.setVersion("0.1-ALPHA");
        openAPI.setInfo(info);
        //final Tag tag = OASFactory.createTag();
        //tag.setName("Fruityvice Proxy");
        //openAPI.setTags(List.of(tag));
        //openAPI.getPaths().addPathItem("/fruityvice", createPathItem());
        //openAPI.getComponents().addSchema("test", createMyObject());

        final Instance<MappingProcessor> mappingProcessorInstance = CDI.current().select(MappingProcessor.class);
        if (mappingProcessorInstance.isResolvable()) {
            final MappingProcessor mappingProcessor = mappingProcessorInstance.get();

            for (MappingListConfig.MappingConfig config : mappingProcessor.getMappings()) {
                //LOG.info("Add mapping: " + normalizePath(config.path) + " -> " + config.backend);
                if (config.openapi != null) {
                    LOG.info("Parsing openapi:" + config.openapi);
                    try {
                        OpenAPI api = parseOpenApi(config.openapi);
                        for (Map.Entry<String, PathItem> pathItemEntry : api.getPaths().entrySet()) {
                            final PathItem pathItem = pathItemEntry.getValue();
                            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : pathItem.getOperations().entrySet()) {
                                operationEntry.getValue().setTags(prefixTags(config.getDescription(), operationEntry.getValue().getTags()));
                            }
                            openAPI.getPaths().addPathItem(normalizePath(config.path) + pathItemEntry.getKey(), pathItem);
                        }
                    } catch (IOException e) {
                        LOG.error("Error parsing openapi definition: " + config.openapi, e);
                    }
                } else if (config.schema != null) {
                    LOG.info("SKipping schema:" + config.schema);
                    //OpenApiParser.parseSchema(config.schema);
                } else {
                    openAPI.getPaths().addPathItem(normalizePath(config.path), createPathItem(config));
                }
            }
        }
    }

    private OpenAPI parseOpenApi(String openapi) throws IOException {
        URL apiUrl = toUrl(openapi);
        final Format format = detectOpenApiFormat(apiUrl);
        if (format != null) {
            return parseOpenApi(apiUrl, format);
        }
        try {
            return parseOpenApi(apiUrl, Format.YAML);
        } catch (IOException e) {
            return parseOpenApi(apiUrl, Format.JSON);
        }
    }

    private URL toUrl(String resource) throws MalformedURLException {
        URL apiUrl;
        if (URL_SCHEME.matcher(resource).find()) {
            apiUrl = new URL(resource);
        } else {
            apiUrl = new File(resource).toURI().toURL();
        }
        return apiUrl;
    }

    private OpenAPI parseOpenApi(URL url, Format format) throws IOException {
        try (InputStream stream = url.openStream()) {
            return OpenApiParser.parse(stream, format);
        }
    }

    private Format detectOpenApiFormat(URL url) {
        String fileName = url.getFile();
        int lidx = fileName.lastIndexOf(46);
        if (lidx != -1 && lidx < fileName.length()) {
            String ext = fileName.substring(lidx + 1);
            boolean isJson = ext.equalsIgnoreCase("json");
            boolean isYaml = ext.equalsIgnoreCase("yaml") || ext.equalsIgnoreCase("yml");
            if (isJson) {
                return Format.JSON;
            } else if (isYaml) {
                return Format.YAML;
            }
        }
        return null;
    }

    private List<String> prefixTags(String prefix, List<String> tags) {
        if (tags == null) {
            tags = Collections.emptyList();
        }
        List result = new ArrayList(tags.size());
        for (String tag: tags) {
            if (DEFAULT_TAG.equals(tag)) {
                result.add(prefix);
            } else {
                result.add(prefix + " - " + tag);
            }
        }
        if (tags.isEmpty()) {
            result.add(prefix);
        }
        return result;
    }

    private PathItem createPathItem(MappingListConfig.MappingConfig config) {
        final PathItem pathItem = OASFactory.createPathItem();
        //pathItem.setRef("MyRef");
        pathItem.GET(createOperation(List.of(config.getDescription())));
        return pathItem;
    }

    private PathItem createPathItem() {
        final PathItem pathItem = OASFactory.createPathItem();
        //pathItem.setRef("MyRef");
        pathItem.GET(createOperation(List.of("Fruityvice Proxy")));
        return pathItem;
    }

    private Operation createOperation(List<String> tags) {
        final Operation operation = OASFactory.createOperation();
        operation.setTags(tags);
        operation.addParameter(createParameter());
        return operation;
    }

    private Parameter createParameter() {
        final Parameter parameter = OASFactory.createParameter();
        //final Content content = OASFactory.createContent();
        //final MediaType mediaType = OASFactory.createMediaType();
        //content.addMediaType("text", mediaType);
        //parameter.content(content);
        parameter.setName("param");
        //parameter.setStyle(Parameter.Style.SIMPLE);
        parameter.description("the parameter");
        /*
        final Example example = OASFactory.createExample();
        example.setValue("bli");
        parameter.addExample("bla", example);
         */
        return parameter;
    }

    private Schema createMyObject() {
        Schema schema = new SchemaImpl("Test");
        schema.setType(Schema.SchemaType.OBJECT);
        schema.setProperties(createProperties());
        return schema;
    }

    private Map<String, Schema> createProperties() {
        Map<String, Schema> map = new HashMap<>();
        map.put("foo", createFoo());
        return map;
    }

    private Schema createFoo() {
        Schema schema = new SchemaImpl("foo");
        schema.setType(Schema.SchemaType.STRING);
        schema.setNullable(Boolean.TRUE);
        return schema;
    }
}
