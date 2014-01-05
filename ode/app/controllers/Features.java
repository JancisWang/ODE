package controllers;

import java.util.List;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import play.data.validation.Constraints.Required;

import models.Feature;
import models.Value;
import views.html.features;

import static play.data.Form.form;



public class Features extends Controller {

    @Security.Authenticated(Secured.class)
    public static Promise<Result> list() {
        Promise<List<Feature>> featureList = Feature.all();
        Promise<List<Value>> globalValueList = Value.all();
        Promise<Tuple<List<Feature>, List<Value>>> lists = featureList
            .zip(globalValueList);
        return lists.map(
            new Function<Tuple<List<Feature>, List<Value>>, Result>() {
                public Result apply(
                    Tuple<List<Feature>, List<Value>> lists) {
                    List<Feature> featureList = lists._1;
                    for (Feature feat: featureList) {
                        feat.values = feat.getValues().get();
                    }
                    List<Value> globalValueList = lists._2;
                    return ok(features.render(featureList,
                                              form(NewFeatureForm.class),
                                              globalValueList));
            }});
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> createFeature() {
        final Form<NewFeatureForm> featureForm =
            form(NewFeatureForm.class).bindFromRequest();
        final Feature feature = new Feature(featureForm.get().name,
                                            featureForm.get().type,
                                            featureForm.get().description);
        if (feature.exists().get()) {
            return Promise.promise(new Function0<Result>() {
                public Result apply() {
                    flash("error", "Feature already exists.");
                    return redirect(routes.Features.list());
                }});
        } else {
            Promise<Feature> newFeature = feature.create();
            return newFeature.map(new Function<Feature, Result>() {
                public Result apply(Feature feature) {
                    if (feature == null) {
                        flash("error", "Feature creation failed.");
                    } else {
                        flash("success", "Feature successfully created.");
                    }
                    return redirect(routes.Features.list());
                }});
        }
    }

    @Security.Authenticated(Secured.class)
    public static Promise<Result> updateFeature(String name) {
        final Form<UpdateFeatureForm> featureForm =
            form(UpdateFeatureForm.class).bindFromRequest();
        final Feature feature = new Feature(name, featureForm.get().type);
        Promise<Feature> updatedFeature = feature.update();
        return updatedFeature.map(new Function<Feature, Result>() {
            public Result apply(Feature feature) {
                if (feature.featureType.equals("complex")) {
                    Feature permittedFeature = new Feature(
                        featureForm.get().feature);
                    feature.connectTo(permittedFeature, "ALLOWS");
                } else if (feature.featureType.equals("atomic")) {
                    String valueName = featureForm.get().value;
                    Value permittedValue = new Value(valueName);
                    if (!permittedValue.exists().get()) {
                        permittedValue.create();
                    }
                    feature.connectTo(permittedValue, "ALLOWS");
                }
                return redirect(routes.Features.list());
            }});
    }

    public static class NewFeatureForm {
        @Required
        public String name;
        @Required
        public String type;
        public String description;
    }

    public static class UpdateFeatureForm {
        public String type;
        public String feature;
        public String value;
    }

}