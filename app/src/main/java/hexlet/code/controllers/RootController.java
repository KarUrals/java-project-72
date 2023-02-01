package hexlet.code.controllers;

import java.io.PrintWriter;
import io.javalin.http.Handler;

public class RootController {
    public static final Handler welcome = ctx -> {
        ctx.render("index.html");
    };
}
