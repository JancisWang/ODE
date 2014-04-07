package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.charset.Charset;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.RHSManager;
import models.relationships.GroupRelationship;
import models.relationships.RHSRelationship;


public class RHS extends UUIDNode {
    public Rule rule;

    private RHS() {
        super(NodeType.RHS);
    }

    public RHS(Rule rule) {
        this();
        this.rule = rule;
    }

    public static RHS of(Rule rule) {
        return new RHS(rule);
    }

    public Promise<UUID> getUUID() {
        Promise<UUID> parentUUID = this.rule.getUUID();
        return parentUUID.map(new UUIDFunction());
    }

    private Promise<List<CombinationGroup>> getGroups() {
        return GroupRelationship.getEndNodes(this);
    }

    public Promise<Boolean> create() {
        Promise<UUID> ruleUUID = this.rule.getUUID();
        Promise<Boolean> created = ruleUUID.flatMap(new CreateFunction(this));
        return created.flatMap(new CreateGroupFunction(this));
    }

    public Promise<Boolean> connectTo(Rule embeddingRule) {
        return new RHSRelationship(embeddingRule, this).create();
    }

    public Promise<Boolean> add(final CombinationGroup group) {
        Promise<Boolean> groupCreated = group.create();
        return groupCreated.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean groupCreated) {
                    if (groupCreated) {
                        Promise<UUID> uuid = RHS.this.getUUID();
                        return uuid.flatMap(
                            new Function<UUID, Promise<Boolean>>() {
                                public Promise<Boolean> apply(UUID uuid) {
                                    RHS.this.jsonProperties
                                        .put("uuid", uuid.toString());
                                    return group.connectTo(RHS.this);
                                }
                            });
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> remove(final CombinationGroup group) {
        Promise<UUID> uuid = this.getUUID();
        return uuid.flatMap(
            new Function<UUID, Promise<Boolean>>() {
                public Promise<Boolean> apply(UUID uuid) {
                    RHS.this.jsonProperties.put("uuid", uuid.toString());
                    return group.removeFrom(RHS.this);
                }
            });
    }

    private Promise<Boolean> removeGroups() {
        Promise<List<CombinationGroup>> groups = this.getGroups();
        Promise<List<Boolean>> removed = groups.flatMap(
            new Function<List<CombinationGroup>, Promise<List<Boolean>>>() {
                public Promise<List<Boolean>> apply(
                    List<CombinationGroup> groups) {
                    List<Promise<? extends Boolean>> removed =
                        new ArrayList<Promise<? extends Boolean>>();
                    for (CombinationGroup group: groups) {
                        removed.add(RHS.this.remove(group));
                    }
                    return Promise.sequence(removed);
                }
            });
        return removed.map(
            new Function<List<Boolean>, Boolean>() {
                public Boolean apply(List<Boolean> removed) {
                    for (Boolean r: removed) {
                        if (!r) {
                            return false;
                        }
                    }
                    return true;
                }
            });
    }

    public Promise<Boolean> delete() {
        Promise<Boolean> groupsRemoved = this.removeGroups();
        return groupsRemoved.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean groupsRemoved) {
                    if (groupsRemoved) {
                        return RHSManager.delete(RHS.this);
                    }
                    return Promise.pure(false);
                }
            });
    }

    private static class UUIDFunction implements Function<UUID, UUID> {
        public UUID apply(UUID ruleUUID) {
            byte[] bytes = ruleUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            return UUID.nameUUIDFromBytes(bytes);
        }
    }

    protected static class CreateFunction
        implements Function<UUID, Promise<Boolean>> {
        private RHS rhs;
        public CreateFunction(RHS rhs) {
            this.rhs = rhs;
        }
        public Promise<Boolean> apply(UUID ruleUUID) {
            byte[] bytes = ruleUUID.toString()
                .getBytes(Charset.forName("UTF-8"));
            UUID uuid = UUID.nameUUIDFromBytes(bytes);
            this.rhs.jsonProperties.put("uuid", uuid.toString());
            return RHSManager.create(this.rhs);
        }
    }

    private static class CreateGroupFunction
        implements Function<Boolean, Promise<Boolean>> {
        private RHS rhs;
        public CreateGroupFunction(RHS rhs) {
            this.rhs = rhs;
        }
        public Promise<Boolean> apply(Boolean created) {
            if (created) {
                final RHS rhs = this.rhs;
                final CombinationGroup group =
                    new CombinationGroup(UUID.randomUUID(), 1);
                Promise<Boolean> groupCreated = group.create();
                return groupCreated.flatMap(
                    new Function<Boolean, Promise<Boolean>>() {
                        public Promise<Boolean> apply(Boolean groupCreated) {
                            if (groupCreated) {
                                return group.connectTo(rhs);
                            }
                            return Promise.pure(false);
                        }
                    });
            }
            return Promise.pure(false);
        }
    }

}
