package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.Application.Login;
import models.nodes.User;
import play.data.Form;
import play.libs.F.Function0;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Content;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.home;
import views.html.login;

import static play.data.Form.form;


public class Auth extends Controller {

    public static Promise<Result> register() {
        final Form<RegistrationForm> registrationForm =
            form(RegistrationForm.class).bindFromRequest();
        if (registrationForm.hasErrors()) {
            return Promise.promise(
                new ErrorResult(home.render(registrationForm)));
        }
        ObjectNode user = Json.newObject();
        user.put("username", registrationForm.get().email);
        user.put("password", registrationForm.get().password);
        Promise<Boolean> created = User.nodes.create(user);
        return created.map(
            new Function<Boolean, Result>() {
                public Result apply(Boolean created) {
                    if (created) {
                        flash("success", "Registration successful.");
                    } else {
                        flash("error", "Registration failed.");
                    }
                    return redirect(routes.Application.home());
                }
            });
    }

    public static Promise<Result> authenticate() {
        final Form<Login> loginForm = form(Login.class).bindFromRequest();
        if (loginForm.hasErrors()) {
            return Promise.promise(new ErrorResult(login.render(loginForm)));
        }
        final ObjectNode user = Json.newObject();
        user.put("username", loginForm.get().email);
        user.put("password", loginForm.get().password);
        Promise<Boolean> exists = User.nodes.exists(user);
        return exists.map(
            new Function<Boolean, Result>() {
                public Result apply(Boolean exists) {
                    if (exists) {
                        session().clear();
                        session("username", user.get("username").asText());
                        flash("success", "Login successful.");
                        return redirect(routes.Application.home());
                    } else {
                        flash("error", "Login failed: Unknown user.");
                        return badRequest(login.render(loginForm));
                }
            }
        });
    }

    private static class ErrorResult implements Function0<Result> {
        private Content content;
        public ErrorResult(Content content) {
            this.content = content;
        }
        public Result apply() {
            return badRequest(content);
        }
    }

    public static class RegistrationForm extends Login {}

}
