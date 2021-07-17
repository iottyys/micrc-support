package io.ttyys.micrc.api.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.spi.RouteTemplateParameterSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ApiRouteTemplateParameterSource implements RouteTemplateParameterSource {
    private final Map<String, ApiRouteConfiguration.AbstractApiDefinition> parameters = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> parameters(String routeId) {
        return new ObjectMapper().convertValue(parameters.get(routeId), LinkedHashMap.class);
    }

    @Override
    public Set<String> routeIds() {
        return parameters.keySet();
    }

    public void addParameter(String routeId, ApiRouteConfiguration.AbstractApiDefinition definition) {
        parameters.put(routeId, definition);
    }

    public ApiRouteConfiguration.AbstractApiDefinition parameter(String routeId) {
        return parameters.get(routeId);
    }
}
