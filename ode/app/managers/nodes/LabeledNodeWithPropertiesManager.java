package managers.nodes;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import managers.functions.JsonFunction;
import managers.functions.NodeCreatedFunction;
import models.nodes.LabeledNodeWithProperties;
import models.nodes.Node;
import neo4play.Neo4jService;


public class LabeledNodeWithPropertiesManager extends LabeledNodeManager {

    public Promise<? extends List<? extends Node>> all() {
        return null;
    }

    public Promise<? extends Node> get(JsonNode properties) {
        return null;
    }

    protected Promise<Boolean> connect(
        JsonNode startNode, JsonNode endNode, String location) {
        return null;
    }

    protected Promise<Boolean> disconnect(
        JsonNode startNode, JsonNode endNode, String location) {
        return null;
    }


    public static Promise<JsonNode> get(LabeledNodeWithProperties node) {
        Promise<WS.Response> response = Neo4jService
            .getLabeledNodeWithProperties(
                node.getLabel(), node.jsonProperties);
        return response.map(new JsonFunction());
    }

}
