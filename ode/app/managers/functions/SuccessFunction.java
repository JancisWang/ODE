package managers.functions;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.WS;
import play.libs.F.Function;


public class SuccessFunction implements Function<WS.Response, Boolean> {

    public Boolean apply(WS.Response response) {
        JsonNode json = response.asJson();
        return json.get("errors").size() == 0;
    }

}