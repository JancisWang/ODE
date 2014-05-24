package models.nodes;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.FeatureType;


public class AtomicFeature extends Feature {

    public AtomicFeature(String name) {
        super(name);
        this.type = FeatureType.ATOMIC;
    }

    public AtomicFeature(String name, String description) {
        super(name, description);
        this.type = FeatureType.ATOMIC;
    }

}
