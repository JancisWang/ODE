package managers.relationships;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import constants.RelationshipType;
import neo4play.Neo4jService;
import models.nodes.Part;
import models.nodes.Slot;


public class HasPartRelationshipManager extends TypedRelationshipManager {

    public static Promise<List<JsonNode>> getEndNodes(final Slot startNode) {
        Promise<List<WS.Response>> responses = Neo4jService
            .getRelationshipTargets(startNode.getLabel(),
                                    startNode.jsonProperties,
                                    RelationshipType.HAS.toString());
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

    public static Promise<Boolean> delete(
        final Slot startNode, final Part endNode) {
        Promise<WS.Response> response = Neo4jService.deleteTypedRelationship(
            startNode, endNode, RelationshipType.HAS);
        return response.map(
            new Function<WS.Response, Boolean>() {
                public Boolean apply(WS.Response response) {
                    return response.getStatus() == Status.OK;
                }
            });
    }

}