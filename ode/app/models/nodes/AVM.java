package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.AVMManager;
import models.relationships.HasFeatureRelationship;


public abstract class AVM extends LabeledNodeWithProperties {
    public Rule rule;
    public JsonNode json;

    protected AVM() {
        this.label = NodeType.AVM;
        this.jsonProperties = Json.newObject();
    }

    public AVM(Rule rule) {
        this();
        this.rule = rule;
    }

    public abstract Promise<UUID> getUUID();

    public abstract Promise<Boolean> create();

    public Promise<List<Feature>> getFeatures() {
        return HasFeatureRelationship.getEndNodes(this);
    }

    public Promise<List<Pair>> getPairs() {
        final AVM avm = this;
        Promise<List<Feature>> features = this.getFeatures();
        return features.flatMap(
            new Function<List<Feature>, Promise<List<Pair>>>() {
                public Promise<List<Pair>> apply(List<Feature> features) {
                    List<Promise<? extends Pair>> pairs =
                        new ArrayList<Promise<? extends Pair>>();
                    for (Feature feature: features) {
                        final String attribute = feature.name;
                        Promise<JsonNode> value = feature
                            .getValue(avm.rule, avm);
                        Promise<Pair> pair = value.map(
                            new Function<JsonNode, Pair>() {
                                public Pair apply(JsonNode value) {
                                    return new Pair(attribute, value);
                                }
                            });
                        pairs.add(pair);
                    }
                    return Promise.sequence(pairs);
                }
            });
    }

    public Promise<JsonNode> toJSON() {
        final AVM avm = this;
        Promise<UUID> uuid = this.getUUID();
        Promise<JsonNode> json = uuid.flatMap(
            new Function<UUID, Promise<JsonNode>>() {
                public Promise<JsonNode> apply(final UUID uuid) {
                    Promise<List<Pair>> pairs = avm.getPairs();
                    Promise<JsonNode> json = pairs.map(
                        new Function<List<Pair>, JsonNode>() {
                            public JsonNode apply(List<Pair> pairs) {
                                ObjectNode json = Json.newObject();
                                json.put("uuid", uuid.toString());
                                for (Pair pair: pairs) {
                                    json.put(pair.attribute, pair.value);
                                }
                                return json;
                            }
                        });
                    return json;
                }
            });
        return json;
    }

    private class Pair {
        public String attribute;
        public JsonNode value;
        public Pair(String attribute, JsonNode value) {
            this.attribute = attribute;
            this.value = value;
        }
    }

    protected class UUIDFunction implements Function<UUID, UUID> {
        public UUID apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

    protected class CreateFunction implements
                                     Function<UUID, Promise<Boolean>> {
        private AVM avm;
        public CreateFunction(AVM avm) {
            this.avm = avm;
        }
        public Promise<Boolean> apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.avm.jsonProperties.put("uuid", uuid.toString());
            return AVMManager.create(this.avm);
        }
    }

}
