package models.nodes;

import java.util.UUID;

import play.libs.F.Promise;

import constants.NodeType;
import managers.nodes.CombinationGroupManager;
import models.relationships.GroupRelationship;


public class CombinationGroup extends LabeledNodeWithProperties {

    private CombinationGroup(UUID uuid) {
        super(NodeType.COMBINATION_GROUP);
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public CombinationGroup(UUID uuid, int position) {
        super(NodeType.COMBINATION_GROUP);
        this.jsonProperties.put("uuid", uuid.toString());
        this.jsonProperties.put("position", position);
    }

    public static CombinationGroup of(String groupID) {
        return new CombinationGroup(UUID.fromString(groupID));
    }

    public Promise<Boolean> create() {
        return CombinationGroupManager.create(this);
    }

    public Promise<Boolean> connectTo(RHS embeddingRHS) {
        return new GroupRelationship(embeddingRHS, this).create();
    }

    public Promise<Boolean> addString(String content) {
        return OutputString.of(content).connectTo(this);
    }

    public Promise<Boolean> updateString(String stringID, String content) {
        return null;
    }

    public Promise<Boolean> removeString(String stringID) {
        return null;
    }

    public Promise<Boolean> addSlot() {
        return null;
    }

    public Promise<Boolean> removeSlot(String slotID) {
        return null;
    }

    public Promise<Boolean> addPart(String slotID, String part) {
        return null;
    }

    public Promise<Boolean> updatePart(
        String slotID, String partID, String content) {
        return null;
    }

    public Promise<Boolean> removePart(String slotID, String partID) {
        return null;
    }

    public Promise<Boolean> addRef(String slotID, String ruleName) {
        return null;
    }

    public Promise<Boolean> delete() {
        return Promise.pure(false);
    }

}
