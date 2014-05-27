package controllers;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import models.nodes.CombinationGroup;
import models.nodes.Feature;
import models.nodes.OutputString;
import models.nodes.Part;
import models.nodes.LHS;
import models.nodes.RHS;
import models.nodes.Rule;
import models.nodes.Slot;
import models.nodes.Substructure;
import utils.UUIDGenerator;
import views.html.browse;
import views.html.details;
import views.html.input;
import views.html.output;


public class Rules extends Controller {

    @Security.Authenticated(Secured.class)
    public static Promise<Result> browse() {
        Promise<List<Rule>> ruleList = Rule.nodes.all();
        return ruleList.map(
            new Function<List<Rule>, Result>() {
                public Result apply(List<Rule> ruleList) {
                    return ok(browse.render(ruleList));
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> details(String name) {
        Promise<Rule> requestedRule = new Rule(name).get();
        return requestedRule.map(
            new Function<Rule, Result>() {
                public Result apply(Rule requestedRule) {
                    return ok(details.render(requestedRule));
                }
            });
    }


    @Security.Authenticated(Secured.class)
    public static Promise<Result> similar(String name) {
        Rule rule = new Rule(name);
        Promise<List<Rule>> ruleList = rule.getSimilarRules();
        return ruleList.map(
            new Function<List<Rule>, Result>() {
                public Result apply(List<Rule> ruleList) {
                    return ok(browse.render(ruleList));
                }
            });
    }


    @Security.Authenticated(Secured.class)
    public static Promise<Result> input(final String name) {
        Promise<List<Feature>> globalFeatureList = Feature.nodes.all();
        Promise<Rule> rule = new Rule(name).get();
        Promise<Tuple<List<Feature>, Rule>> results = globalFeatureList
            .zip(rule);
        return results.map(
            new Function<Tuple<List<Feature>, Rule>, Result>() {
                public Result apply(Tuple<List<Feature>, Rule> results) {
                    return ok(input.render(results._1, results._2));
                }
            });
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> output(final String name) {
        Promise<List<Part>> globalPartsList = Part.nodes.all();
        Promise<Rule> rule = new Rule(name).get();
        Promise<Tuple<List<Part>, Rule>> results = globalPartsList
            .zip(rule);
        return results.map(
            new Function<Tuple<List<Part>, Rule>, Result>() {
                public Result apply(Tuple<List<Part>, Rule> results) {
                    return ok(output.render(results._1, results._2));
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> lhs(String name) {
        Rule rule = new Rule(name);
        Promise<JsonNode> lhsJSON = new LHS(rule).toJSON();
        return lhsJSON.map(
            new Function<JsonNode, Result>() {
                public Result apply(JsonNode lhsJSON) {
                    ObjectNode result = Json.newObject();
                    result.put("json", lhsJSON);
                    return ok(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> rhs(String name) {
        Rule rule = new Rule(name);
        Promise<JsonNode> rhsJSON = new RHS(rule).toJSON();
        return rhsJSON.map(
            new Function<JsonNode, Result>() {
                public Result apply(JsonNode rhsJSON) {
                    ObjectNode result = Json.newObject();
                    result.put("json", rhsJSON);
                    return ok(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> create() {
        final JsonNode json = request().body().asJson();
        Promise<Boolean> created = Rule.nodes.create(json);
        return created.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean created) {
                    if (created) {
                        String name = json.get("name").asText();
                        result.put("id", name);
                        result.put("name", name);
                        result.put(
                            "description", json.get("description").asText());
                        return ok(result);
                    }
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateName(final String name) {
        final ObjectNode newProps = (ObjectNode) request().body().asJson();
        Promise<Boolean> nameTaken =
            Rule.nodes.exists(newProps.deepCopy().retain("name"));
        Promise<Boolean> updated = nameTaken.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean nameTaken) {
                    if (nameTaken) {
                        return Promise.pure(false);
                    }
                    ObjectNode oldProps = Json.newObject();
                    oldProps.put("name", name);
                    newProps.retain("uuid", "name", "description");
                    return Rule.nodes.update(oldProps, newProps);
                }
            });
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("id", newProps.get("name").asText());
                        result.put("message", "Name successfully updated.");
                        return ok(result);
                    }
                    result.put("message", "Name not updated.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateDescription(String name) {
        ObjectNode newProps = (ObjectNode) request().body().asJson();
        newProps.retain("uuid", "name", "description");
        ObjectNode oldProps = newProps.deepCopy().retain("name");
        Promise<Boolean> updated = Rule.nodes.update(oldProps, newProps);
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("message",
                                   "Description successfully updated.");
                        return ok(result);
                    }
                    result.put("message",
                               "Description not updated.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addFeature(String name) {
        JsonNode json = request().body().asJson();
        final ObjectNode avm = (ObjectNode) json.deepCopy();
        avm.retain("ruleUUID", "uuid");
        final ObjectNode feature = Json.newObject();
        feature.put("name", json.findValue("name").asText());
        feature.put("type", json.findValue("type").asText());
        Promise<Boolean> added = Substructure.nodes.connect(avm, feature);
        return added.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean added) {
                    if (added) {
                        String type = feature.get("type").asText();
                        if (type.equals("complex")) {
                            String parentUUID = avm.get("uuid").asText();
                            String fname = feature.get("name").asText();
                            String uuid = UUIDGenerator
                                .from(parentUUID + fname);
                            ObjectNode value = Json.newObject();
                            value.put("uuid", uuid);
                            value.putArray("pairs");
                            result.put("value", value);
                        } else {
                            result.put(
                                "value", new TextNode("underspecified"));
                        }
                        result.put("message", "Feature successfully added.");
                        return ok(result);
                    }
                    result.put("message", "Feature not added.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateFeatureValue(String name) {
        JsonNode json = request().body().asJson();
        ObjectNode avm = (ObjectNode) json.deepCopy();
        avm.retain("ruleUUID", "uuid");
        ObjectNode feature = Json.newObject();
        feature.put("name", json.findValue("name").asText());
        ObjectNode value = Json.newObject();
        value.put("name", json.findValue("value").asText());
        ObjectNode newValue = Json.newObject();
        newValue.put("name", json.findValue("newValue"));
        Promise<Boolean> updated = Substructure.nodes
            .setValue(avm, feature, value, newValue);
        return updated.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean updated) {
                    if (updated) {
                        result.put("message",
                                   "Feature successfully updated.");
                        return ok(result);
                    }
                    result.put("message", "Feature not updated.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeFeature(String name) {
        JsonNode json = request().body().asJson();
        ObjectNode avm = (ObjectNode) json.deepCopy();
        avm.retain("ruleUUID", "uuid");
        ObjectNode feature = Json.newObject();
        feature.put("name", json.findValue("name").asText());
        feature.put("type", json.findValue("type").asText());
        Promise<Boolean> removed = Substructure.nodes
            .removeFeature(avm, feature);
        return removed.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean removed) {
                    if (removed) {
                        result.put("message",
                                   "Feature successfully removed.");
                        return ok(result);
                    }
                    result.put("message", "Feature not removed.");
                    return badRequest(result);
                }
            });
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addString(String name, String groupID) {
        JsonNode json = request().body().asJson();
        ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        ObjectNode string = Json.newObject();
        String uuid = UUIDGenerator.from(json.get("content").asText());
        string.put("uuid", uuid);
        string.put("content", json.get("content").asText());
        Promise<Boolean> connected = CombinationGroup.nodes
            .connect(group, string);
        ObjectNode result = Json.newObject();
        result.put("id", uuid);
        return connected.map(new ResultFunction(
                                 "String successfully added.",
                                 "String not added.", result));

    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateString(
        String name, String groupID, String stringID) {
        final ObjectNode result = Json.newObject();
        final ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        final ObjectNode string = Json.newObject();
        string.put("uuid", stringID);
        Promise<Boolean> updated = CombinationGroup.nodes
            .disconnect(group, string);
        updated = updated.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean updated) {
                    if (updated) {
                        JsonNode json = request().body().asJson();
                        String content = json.findValue("content").asText();
                        String uuid = UUIDGenerator.from(content);
                        ObjectNode string = Json.newObject();
                        string.put("content", content);
                        string.put("uuid", uuid);
                        result.put("id", uuid);
                        return CombinationGroup.nodes.connect(group, string);
                    }
                    return Promise.pure(false);
                }
            });
        updated.onRedeem(
            new Callback<Boolean>() {
                public void invoke(Boolean updated) {
                    if (updated) {
                        OutputString.nodes.delete(string);
                    }
                }
            });
        return updated.map(
            new ResultFunction("String successfully updated.",
                               "String not updated.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeString(
        String name, String groupID, String stringID) {
        ObjectNode group = Json.newObject();
        group.put("uuid", groupID);
        final ObjectNode string = Json.newObject();
        string.put("uuid", stringID);
        Promise<Boolean> removed = CombinationGroup.nodes
            .disconnect(group, string);
        removed.onRedeem(
            new Callback<Boolean>() {
                public void invoke(Boolean removed) {
                    if (removed) {
                        OutputString.nodes.delete(string);
                    }
                }
            });
        return removed.map(new ResultFunction("String successfully removed.",
                                              "String not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addGroup(String name) {
        final ObjectNode result = Json.newObject();
        final JsonNode json = request().body().asJson();
        final ObjectNode group = Json.newObject();
        final String uuid = UUIDGenerator.random();
        group.put("uuid", uuid);
        group.put("position", json.findValue("position").asInt());
        Promise<Boolean> created = CombinationGroup.nodes.create(group);
        Promise<Boolean> added = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        ObjectNode rhs = Json.newObject();
                        rhs.put("uuid", json.findValue("rhsID").asText());
                        result.put("id", uuid);
                        return RHS.nodes.connect(rhs, group);
                    }
                    return Promise.pure(false);
                }
            });
        return added.map(new ResultFunction("Group successfully added.",
                                            "Group not added.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updateGroup(String name, String groupID) {
        JsonNode json = request().body().asJson();
        ObjectNode oldProps = Json.newObject();
        oldProps.put("uuid", groupID);
        ObjectNode newProps = oldProps.deepCopy();
        newProps.put("position", json.findValue("position").asInt());
        Promise<Boolean> updated = CombinationGroup.nodes
            .update(oldProps, newProps);
        return updated.map(new ResultFunction("Group successfully updated.",
                                              "Group not updated."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeGroup(String name, String groupID) {
        CombinationGroup group = CombinationGroup.of(groupID);
        Promise<Boolean> removed = new Rule(name).removeGroup(group);
        return removed.map(new ResultFunction("Group successfully removed.",
                                              "Group not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addSlot(String name, final String groupID) {
        final ObjectNode result = Json.newObject();
        JsonNode json = request().body().asJson();
        final ObjectNode slot = Json.newObject();
        final String uuid = UUIDGenerator.random();
        int position = json.findValue("position").asInt();
        slot.put("uuid", uuid);
        slot.put("position", position);
        Promise<Boolean> created = Slot.nodes.create(slot);
        Promise<Boolean> added = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        result.put("id", uuid);
                        ObjectNode group = Json.newObject();
                        group.put("uuid", groupID);
                        return CombinationGroup.nodes.connect(group, slot);
                    }
                    return Promise.pure(false);
                }
            });
        return added.map(new ResultFunction("Slot successfully added.",
                                            "Slot not added", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeSlot(
        String name, String groupID, String slotID) {
        Slot slot = Slot.of(UUID.fromString(slotID));
        Promise<Boolean> removed = CombinationGroup.of(groupID)
            .removeSlot(slot);
        return removed.map(new ResultFunction("Slot successfully removed.",
                                              "Slot not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addPart(
        String name, String groupID, String slotID) {
        JsonNode json = request().body().asJson();
        ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        ObjectNode part = Json.newObject();
        String content = json.findValue("content").asText();
        String uuid = UUIDGenerator.from(content);
        part.put("uuid", uuid);
        part.put("content", content);
        Promise<Boolean> connected = Slot.nodes.connect(slot, part);
        ObjectNode result = Json.newObject();
        result.put("id", uuid);
        return connected.map(new ResultFunction("Part successfully added.",
                                                "Part not added.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> updatePart(
        String name, String groupID, String slotID, String partID) {
        final ObjectNode result = Json.newObject();
        final ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        final ObjectNode part = Json.newObject();
        part.put("uuid", partID);
        Promise<Boolean> updated = Slot.nodes.disconnect(slot, part);
        updated = updated.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean updated) {
                    if (updated) {
                        JsonNode json = request().body().asJson();
                        String content = json.findValue("content").asText();
                        String uuid = UUIDGenerator.from(content);
                        ObjectNode part = Json.newObject();
                        part.put("content", content);
                        part.put("uuid", uuid);
                        result.put("id", uuid);
                        return Slot.nodes.connect(slot, part);
                    }
                    return Promise.pure(false);
                }
            });
        updated.onRedeem(
            new Callback<Boolean>() {
                public void invoke(Boolean updated) {
                    if (updated) {
                        Part.nodes.delete(part);
                    }
                }
            });
        return updated.map(new ResultFunction("Part successfully updated.",
                                              "Part not updated.", result));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removePart(
        String name, String groupID, String slotID, String partID) {
        ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        final ObjectNode part = Json.newObject();
        part.put("uuid", partID);
        Promise<Boolean> removed = Slot.nodes.disconnect(slot, part);
        removed.onRedeem(
            new Callback<Boolean>() {
                public void invoke(Boolean removed) {
                    if (removed) {
                        Part.nodes.delete(part);
                    }
                }
            });
        return removed.map(new ResultFunction("Part successfully removed.",
                                              "Part not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> addRef(
        String name, String groupID, String slotID) {
        JsonNode json = request().body().asJson();
        String ruleName = json.findValue("ruleName").asText();
        Promise<Boolean> added;
        if (name.equals(ruleName)) {
            added = Promise.pure(false);
            return added.map(new ResultFunction(
                                 "Cross-reference successfully added.",
                                 "Can't add circular dependency."));
        } else {
            ObjectNode result = Json.newObject();
            result.put("id", ruleName);
            ObjectNode slot = Json.newObject();
            slot.put("uuid", slotID);
            ObjectNode rule = Json.newObject();
            rule.put("name", ruleName);
            added = Slot.nodes.connect(slot, rule);
            return added.map(new ResultFunction(
                                 "Cross-reference successfully added.",
                                 "Cross-reference not added.", result));
        }
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> removeRef(
        String name, String groupID, String slotID, String refID) {
        ObjectNode slot = Json.newObject();
        slot.put("uuid", slotID);
        ObjectNode rule = Json.newObject();
        rule.put("name", refID);
        Promise<Boolean> removed = Slot.nodes.disconnect(slot, rule);
        return removed.map(new ResultFunction("Part successfully removed.",
                                              "Part not removed."));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> delete(String name) {
        Promise<Boolean> deleted = new Rule(name).deleteIfOrphaned();
        return deleted.map(
            new Function<Boolean, Result>() {
                ObjectNode result = Json.newObject();
                public Result apply(Boolean deleted) {
                    if (deleted) {
                        return ok(result);
                    }
                    return badRequest(result);
                }
            });
    }

    private static class ResultFunction implements Function<Boolean, Result> {
        private String successMsg;
        private String errorMsg;
        private ObjectNode result;
        public ResultFunction(String successMsg, String errorMsg) {
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
        public ResultFunction(String successMsg,
                              String errorMsg,
                              ObjectNode result) {
            this(successMsg, errorMsg);
            this.result = result;
        }
        public Result apply(Boolean actionSuccessful) {
            ObjectNode result =
                (this.result == null) ? Json.newObject() : this.result;
            if (actionSuccessful) {
                result.put("message", successMsg);
                return ok(result);
            }
            result.put("message", errorMsg);
            return badRequest(result);
        }
    }

}
