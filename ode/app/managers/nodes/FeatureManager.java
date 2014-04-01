package managers.nodes;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.UpdatedFunction;
import models.nodes.Feature;


public class FeatureManager extends NamedNodeManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.FEATURE);
    }

    public static Promise<Boolean> create(Feature feature) {
        feature.jsonProperties.put("type", feature.getType());
        feature.jsonProperties.put("description", feature.getDescription());
        return LabeledNodeWithPropertiesManager.create(feature);
    }

    public static Promise<List<JsonNode>> getValues(Feature feature) {
        Promise<List<WS.Response>> responses = Neo4jService
            .getRelationshipTargets(feature.getLabel(),
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

    public static Promise<Boolean> updateName(
        Feature feature, String newName) {
        feature.jsonProperties.put("type", feature.getType());
        feature.jsonProperties.put("description", feature.getDescription());
        return NamedNodeManager.updateName(feature, newName);
    }

    public static Promise<Boolean> updateDescription(
        Feature feature, String newDescription) {
        feature.jsonProperties.put("type", feature.getType());
        ObjectNode newProps = feature.jsonProperties.deepCopy();
        newProps.put("description", newDescription);
        return LabeledNodeWithPropertiesManager
            .updateProperties(feature, newProps);
    }

    public static Promise<Boolean> updateType(
        Feature feature, String newType) {
        feature.jsonProperties.put(
            "description", feature.getDescription());
        ObjectNode newProps = feature.jsonProperties.deepCopy();
        newProps.put("type", newType);
        return LabeledNodeWithPropertiesManager
            .updateProperties(feature, newProps);
    }

}
