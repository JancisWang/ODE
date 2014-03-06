package managers;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import models.Rule;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.NodeDeletedFunction;
import managers.functions.PropertyFunction;
import managers.functions.UpdatedFunction;


public class RuleManager extends LabeledNodeWithPropertiesManager {

    public static Promise<List<JsonNode>> all() {
        return LabeledNodeManager.all(NodeType.RULE);
    }

    public static Promise<Boolean> create(Rule rule) {
        rule.jsonProperties.put("description", rule.description);
        return LabeledNodeWithPropertiesManager.create(rule);
    }

    public static Promise<String> getProperty(Rule rule, String propName) {
        Promise<String> ruleURL = Neo4jService
            .getNodeURL(rule.label.toString(), rule.jsonProperties);
        Promise<WS.Response> response = ruleURL.flatMap(
            new Function<String, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(String ruleURL) {
                    return Neo4jService.getNodeProperties(ruleURL);
                }
            });
        Promise<JsonNode> json = response.map(new JsonFunction());
        return json.map(new PropertyFunction(propName));
    }

    public static Promise<UUID> getUUID(Rule rule) {
        Promise<String> prop = getProperty(rule, "uuid");
        return prop.map(
            new Function<String, UUID>() {
                public UUID apply(String prop) {
                    return UUID.fromString(prop);
                }
            });
    }

    public static Promise<Boolean> updateName(Rule rule, String newName) {
        rule.jsonProperties.put("description", rule.description);
        ObjectNode newProps = rule.jsonProperties.deepCopy()
            .retain("uuid", "description");
        newProps.put("name", newName);
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            rule.label.toString(), rule.jsonProperties, newProps);
        return response.map(new UpdatedFunction());
    }

    public static Promise<Boolean> updateDescription(
        Rule rule, String newDescription) {
        ObjectNode newProps = rule.jsonProperties.deepCopy();
        newProps.put("description", newDescription);
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            rule.label.toString(), rule.jsonProperties, newProps);
        return response.map(new UpdatedFunction());
    }

    public static Promise<Boolean> delete(Rule rule) {
        Promise<WS.Response> response = Neo4jService
            .deleteLabeledNodeWithProperties(rule.label.toString(),
                                             rule.jsonProperties);
        return response.map(new NodeDeletedFunction());
    }

}
