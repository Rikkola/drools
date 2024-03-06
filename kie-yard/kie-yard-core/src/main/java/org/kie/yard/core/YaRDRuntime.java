package org.kie.yard.core;

import java.io.InputStream;
import java.util.Map;

import org.drools.util.IoUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class YaRDRuntime {
    private YaRDDefinitions units;
    private JsonMapper jsonMapper = JsonMapper.builder().build();

    public YaRDRuntime(String file) {
        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream(file);
            byte[] bytes = IoUtils.readBytesFromInputStream(resourceAsStream, true);
            String yamlDecision = new String(bytes);

            YaRDParser parser = new YaRDParser(yamlDecision);
            units = parser.getDefinitions();
        } catch (Exception e) {
            // TODO
        }
    }

    public YaRDResult evaluate(String jsonInputCxt) throws Exception {

        Map<String, Object> inputContext = readJSON(jsonInputCxt);
        Map<String, Object> tempOutCtx = units.evaluate(inputContext);
        final String OUTPUT_JSON = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tempOutCtx);
        Map<String, Object> outputJSONasMap = readJSON(OUTPUT_JSON);

        return new YaRDResult(outputJSONasMap);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJSON(final String CONTEXT) throws JsonProcessingException, JsonMappingException {
        return jsonMapper.readValue(CONTEXT, Map.class);
    }
}
