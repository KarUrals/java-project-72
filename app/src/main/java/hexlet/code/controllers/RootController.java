package hexlet.code.controllers;

import java.io.PrintWriter;
import io.javalin.http.Handler;

public class RootController {
    public static Handler welcome = ctx -> {
        PrintWriter printWriter = ctx.res().getWriter();
        printWriter.write("Hello World");
    };
}
