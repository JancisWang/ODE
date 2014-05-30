package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.RelationshipType;
import java.util.ArrayList;
import java.util.List;
import managers.functions.JsonFunction;
import models.functions.ExistsFunction;
import models.nodes.Feature;
import models.nodes.OntologyNode;
import models.nodes.Value;
import models.relationships.Allows;
import models.relationships.Untyped;
import neo4play.Neo4jService;
import neo4play.RelationshipService;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public class FeatureManager extends LabeledNodeWithPropertiesManager {

    public FeatureManager() {
        this.label = "Feature";
    }

    // READ

    @Override
    public Promise<Boolean> exists(JsonNode properties) {
        return super.exists(properties, "name");
    }

    public Promise<List<Feature>> all() {
        Promise<List<JsonNode>> nodes = all(this.label);
        Promise<List<Feature>> features = nodes.flatMap(
            new Function<List<JsonNode>, Promise<List<Feature>>>() {
                public Promise<List<Feature>> apply(List<JsonNode> nodes) {
                    List<Promise<? extends Feature>> features =
                        new ArrayList<Promise<? extends Feature>>();
                    for (JsonNode node: nodes) {
                        String name = node.get("name").asText();
                        String description = "";
                        if (node.has("description")) {
                            description = node.get("description").asText();
                        }
                        String type = node.get("type").asText();
                        final Feature f =
                            new Feature(name, description, type);
                        Promise<List<String>> targets = targets(node);
                        Promise<Feature> feature = targets.map(
                            new Function<List<String>, Feature>() {
                                public Feature apply(List<String> targets) {
                                    f.targets = targets;
                                    return f;
                                }
                            });
                        features.add(feature);
                    }
                    return Promise.sequence(features);
                }
            });
        return features;
    }

    public Promise<Feature> get(JsonNode properties) {
        Promise<JsonNode> json = get(this.label, properties);
        return json.map(
            new Function<JsonNode, Feature>() {
                public Feature apply(JsonNode json) {
                    String name = json.findValue("name").asText();
                    String description = "";
                    if (json.has("description")) {
                        description = json.findValue("description").asText();
                    }
                    String type = json.findValue("type").asText();
                    return new Feature(name, description, type);
                }
            });
    }

    // CREATE

    @Override
    protected Promise<Boolean> create(
        final JsonNode properties, final String location) {
        Promise<Boolean> created = super.create(properties, location, "name");
        if (properties.get("type").asText().equals("complex")) {
            return created;
        }
        return created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        String name = properties.get("name").asText();
                        return Allows.relationships.create(
                            new Feature(name), new Value("underspecified"),
                            location);
                    }
                    return Promise.pure(false);
                }
            });
    }

    // UPDATE

    public Promise<Boolean> setType(final JsonNode properties) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> updated = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> updated =
                        setType(properties, location);
                    return updated.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean updated) {
                                if (updated) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return updated;
    }

    private Promise<Boolean> setType(
        final JsonNode properties, final String location) {
        final Feature feature = new Feature(properties.get("name").asText());
        // 1. Delete all outgoing :ALLOWS relationships
        Promise<Boolean> deleted = Allows.relationships
            .delete(feature, location);
        // 2. Update type
        Promise<Boolean> updated = deleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean deleted) {
                    if (deleted) {
                        ObjectNode oldProps =
                            (ObjectNode) properties.deepCopy();
                        oldProps.retain("name");
                        return update(oldProps, properties, location);
                    }
                    return Promise.pure(false);
                }
            });
        String type = properties.get("type").asText();
        // 3. If new type == "atomic", connect to "underspecified"
        if (type.equals("atomic")) {
            updated = updated.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(
                        Boolean updated) {
                        if (updated) {
                            Value value = new Value("underspecified");
                            return Allows.relationships
                                .create(feature, value, location);
                        }
                        return Promise.pure(false);
                    }
                });
        }
        // 4. If previous type == "atomic", delete orphans
        if (type.equals("complex")) {
            updated.onRedeem(
                new Callback<Boolean>() {
                    public void invoke(Boolean updated) {
                        if (updated) {
                            Value.nodes.delete();
                        }
                    }
                });
        }
        return updated;
    }

    // DELETE

    @Override
    protected Promise<Boolean> delete(
        final JsonNode properties, final String location) {
        Promise<Feature> feature = get(properties);
        Promise<Boolean> deleted = feature.flatMap(
            new Function<Feature, Promise<Boolean>>() {
                public Promise<Boolean> apply(Feature feature) {
                    // 1. Delete all outgoing :ALLOWS relationships
                    Promise<Boolean> deleted = Allows.relationships
                        .delete(feature, location);
                    // 2. Delete feature
                    deleted = deleted.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean deleted) {
                                if (deleted) {
                                    return FeatureManager.super.
                                        delete(properties, location);
                                }
                                return Promise.pure(false);
                            }
                        });
                    // 3. If type == "atomic", delete orphans
                    if (feature.getType().equals("atomic")) {
                        deleted.onRedeem(
                            new Callback<Boolean>() {
                                public void invoke(Boolean deleted) {
                                    if (deleted) {
                                        Value.nodes.delete();
                                    }
                                }
                            });
                    }
                    return deleted;
                }
            });
        return deleted;
    }

    // Connections to other nodes

    protected Promise<Boolean> connect(
        JsonNode feature, final JsonNode target, final String location) {
        String fname = feature.get("name").asText();
        String tname = target.get("name").asText();
        final Feature f = new Feature(fname);
        if (feature.get("type").asText().equals("complex")) {
            return Allows.relationships
                .create(f, new Feature(tname), location);
        }
        final Value v = new Value(tname);
        Promise<Boolean> exists = Value.nodes.exists(target);
        Promise<Boolean> connected = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Allows.relationships.create(f, v, location);
                    }
                    Promise<Boolean> created = Value.nodes
                        .create(target, location);
                    return created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return Allows.relationships
                                        .create(f, v, location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return connected;
    }

    protected Promise<Boolean> disconnect(
        JsonNode feature, final JsonNode target, final String location) {
        String fname = feature.get("name").asText();
        String tname = target.get("name").asText();
        Feature f = new Feature(fname);
        if (feature.get("type").asText().equals("complex")) {
            return Allows.relationships
                .delete(f, new Feature(tname), location);
        }
        Value v = new Value(tname);
        Promise<Boolean> disconnected = Allows.relationships
            .delete(f, v, location);
        disconnected.onRedeem(
            new Callback<Boolean>() {
                public void invoke(Boolean disconnected) {
                    if (disconnected) {
                        Value.nodes.delete(target);
                    }
                }
            });
        return disconnected;

    }

    public Promise<Boolean> orphaned(JsonNode properties) {
        Feature feature = new Feature(properties.get("name").asText());
        return super.orphaned(feature, Untyped.relationships);
    }

    // Custom functionality

    public Promise<Boolean> has(JsonNode feature, JsonNode value) {
        Feature f = new Feature(feature.get("name").asText());
        OntologyNode v;
        String vname = value.get("name").asText();
        if (feature.get("type").asText().equals("complex")) {
            v = new Feature(vname);
        } else {
            v = new Value(vname);
        }
        Promise<WS.Response> response = RelationshipService
            .getRelationshipVariableLength(f, v, "HAS", 1, 2);
        Promise<JsonNode> json = response.map(new JsonFunction());
        return json.map(new ExistsFunction());
    }

    protected Promise<List<String>> targets(JsonNode feature) {
        Feature f = new Feature(feature.get("name").asText());
        Promise<List<JsonNode>> nodes = Allows.relationships.endNodes(f);
        Promise<List<String>> targets = nodes.map(
            new Function<List<JsonNode>, List<String>>() {
                public List<String> apply(List<JsonNode> nodes) {
                    List<String> targets = new ArrayList<String>();
                    for (JsonNode node: nodes) {
                        targets.add(node.get("name").asText());
                    }
                    return targets;
                }
            });
        return targets;
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

    public static Promise<JsonNode> getRules(Feature feature) {
        Promise<WS.Response> response = Neo4jService
            .findEmbeddingNodesAnyDepth(feature, "Rule");
        return response.map(new JsonFunction());
    }

    public static Promise<JsonNode> getRules(Feature feature, Value value) {
        String query = String.format(
            "MATCH (e:Rule)-[:LHS]->()-[*]->(s:Feature)-[r:HAS]->(v:Value) " +
            "WHERE s.name = '%s' AND v.name='%s' AND r.rule = e.uuid " +
            "RETURN e",
            feature.name, value.name);
        Promise<WS.Response> response = Neo4jService
            .executeCustomQuery(query);
        return response.map(new JsonFunction());
    }

}
