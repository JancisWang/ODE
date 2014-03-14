package models.nodes;

import java.util.UUID;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import managers.nodes.AVMManager;
import models.relationships.LHSRelationship;


public class LHS extends AVM {
    public Rule parent;

    public LHS(Rule rule) {
        super(rule);
        this.parent = rule;
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.map(new UUIDFunction());
    }

    public Promise<Boolean> create() {
        Promise<UUID> parentUUID = this.parent.getUUID();
        return parentUUID.flatMap(new CreateFunction(this));
    }

    public Promise<Boolean> connectTo(Rule embeddingRule) {
        return new LHSRelationship(embeddingRule, this).create();
    }

    public Promise<LHS> get() {
        Promise<JsonNode> json = this.toJSON();
        final LHS lhs = this;
        return json.map(
            new Function<JsonNode, LHS>() {
                public LHS apply(JsonNode json) {
                    lhs.json = json;
                    return lhs;
                }
            });
    }

    public Promise<Boolean> add(final Feature feature, final UUID uuid) {
        final LHS lhs = this;
        Promise<UUID> lhsUUID = this.getUUID();
        return lhsUUID.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID lhsUUID) {
                    if (lhsUUID.equals(uuid)) {
                        lhs.jsonProperties.put("uuid", lhsUUID.toString());
                        return lhs.add(feature);
                    }
                    return new Substructure(lhs.rule, uuid).add(feature);
                }
            });
    }

    public Promise<JsonNode> getValue(
        final Feature feature, final UUID uuid) {
        final LHS lhs = this;
        Promise<UUID> lhsUUID = this.getUUID();
        return lhsUUID.flatMap(
            new Function<UUID, Promise<JsonNode>>() {
                public Promise<JsonNode> apply(UUID lhsUUID) {
                    if (lhsUUID.equals(uuid)) {
                        lhs.jsonProperties.put("uuid", lhsUUID.toString());
                        return feature.getValue(lhs.rule, lhs);
                    }
                    Substructure substructure =
                        new Substructure(lhs.rule, uuid);
                    return feature.getValue(lhs.rule, substructure);
                }
            });
    }

    public Promise<Boolean> update(
        final Feature feature, final UUID uuid, final Value newValue) {
        final LHS lhs = this;
        Promise<UUID> lhsUUID = this.getUUID();
        return lhsUUID.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID lhsUUID) {
                    if (lhsUUID.equals(uuid)) {
                        lhs.jsonProperties.put("uuid", lhsUUID.toString());
                        return feature.setValue(newValue, lhs.rule, lhs);
                    }
                    Substructure substructure =
                        new Substructure(lhs.rule, uuid);
                    return feature.setValue(newValue, lhs.rule, substructure);
                }
            });
    }

    public Promise<Boolean> remove(final Feature feature, final UUID uuid) {
        final LHS lhs = this;
        Promise<UUID> lhsUUID = this.getUUID();
        return lhsUUID.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID lhsUUID) {
                    if (lhsUUID.equals(uuid)) {
                        lhs.jsonProperties.put("uuid", lhsUUID.toString());
                        return feature.remove(lhs.rule, lhs);
                    }
                    Substructure substructure =
                        new Substructure(lhs.rule, uuid);
                    return feature.remove(lhs.rule, substructure);
                }
            });
    }

    protected static class UUIDFunction implements Function<UUID, UUID> {
        public UUID apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

    protected static class CreateFunction
        implements Function<UUID, Promise<Boolean>> {
        private LHS lhs;
        public CreateFunction(LHS lhs) {
            this.lhs = lhs;
        }
        public Promise<Boolean> apply(UUID parentUUID) {
            byte[] bytes = parentUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.lhs.jsonProperties.put("uuid", uuid.toString());
            return AVMManager.create(this.lhs);
        }
    }

}
