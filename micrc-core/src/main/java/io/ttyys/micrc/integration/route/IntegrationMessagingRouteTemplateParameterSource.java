package io.ttyys.micrc.integration.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.spi.RouteTemplateParameterSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.ttyys.micrc.integration.route.IntegrationMessagingRouteConfiguration.AbstractIntegrationMessagingDefinition;

public class IntegrationMessagingRouteTemplateParameterSource implements RouteTemplateParameterSource {
    private final Map<String, AbstractIntegrationMessagingDefinition> parameters = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> parameters(String routeId) {
        return new ObjectMapper().convertValue(parameters.get(routeId), LinkedHashMap.class);
    }

    @Override
    public Set<String> routeIds() {
        return parameters.keySet();
    }

    public void addParameter(String routeId, AbstractIntegrationMessagingDefinition definition) {
        parameters.put(routeId, definition);
    }

    public AbstractIntegrationMessagingDefinition parameter(String routeId) {
        return parameters.get(routeId);
    }
}
