package managers;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import constants.RelationshipType;
import models.Feature;
import neo4play.Neo4jService;


public class FeatureManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.FEATURE);
    }

    public static Promise<Boolean> create(Feature feature) {
        feature.jsonProperties.put("type", feature.getType());
        feature.jsonProperties.put("description", feature.getDescription());
        return LabeledNodeWithPropertiesManager.create(feature);
    }

    public static Promise<List<JsonNode>> values(Feature feature) {
        Promise<List<WS.Response>> responses = Neo4jService
            .getRelationshipTargets(feature.label.toString(),
                                    feature.jsonProperties,
                                    RelationshipType.ALLOWS.toString());
        return responses.map(
            new Function<List<WS.Response>, List<JsonNode>>() {
                public List<JsonNode> apply(List<WS.Response> responses) {
                    List<JsonNode> nodes = new ArrayList<JsonNode>();
                    for (WS.Response response: responses) {
                        JsonNode json = response.asJson();
                        nodes.add(json.findValue("data"));
                    }
                    return nodes;
                }
            });
    }

    public static Promise<Boolean> updateType(
        Feature feature, String newType) {
        feature.jsonProperties.put(
            "description", feature.getDescription());
        ObjectNode newProps = feature.jsonProperties.deepCopy();
        newProps.put("type", newType);
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            feature.label.toString(), feature.jsonProperties, newProps);
        return response.map(new managers.functions.UpdatedFunction());
    }

}