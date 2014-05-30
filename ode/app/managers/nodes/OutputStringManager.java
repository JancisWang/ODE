package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class OutputStringManager extends ContentNodeManager {

    public OutputStringManager() {
        this.label = "OutputString";
    }

    // Custom functionality

    protected JsonNode toJSON(JsonNode properties) {
        ArrayNode tokens = JsonNodeFactory.instance.arrayNode();
        String content = properties.get("content").asText();
        String[] contentTokens = content.split(" ");
        for (String token: contentTokens) {
            tokens.add(token);
        }
        ((ObjectNode) properties).put("tokens", tokens);
        return properties;
    }

}
