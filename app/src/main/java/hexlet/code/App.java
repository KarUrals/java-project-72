package hexlet.code;

import hexlet.code.controllers.RootController;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    final Logger logger = LoggerFactory.getLogger(App.class);
    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

    public static Javalin getApp() {

        // Создаём приложение
        Javalin app = Javalin.create();

        addRoutes(app);

        // Обработчик before запускается перед каждым запросом
        // Устанавливаем атрибут ctx для запросов
        app.before(ctx -> ctx.attribute("ctx", ctx));

        return app;
    }

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        return Integer.valueOf(port);
    }

    private static void addRoutes(Javalin app) {
        app.get("/", RootController.welcome);
    }

    public void print() {
        System.out.print("KAR test");
    }
}
