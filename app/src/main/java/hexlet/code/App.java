package hexlet.code;

import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlController;

import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.thymeleaf.TemplateEngine;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public final class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

    public static Javalin getApp() {

        // Создаём приложение
        Javalin app = Javalin.create(config -> {
            if (!isProduction()) {
                // Включаем логгирование
                config.enableDevLogging();
            }
            config.enableWebjars();
            // Подключаем настроенный шаблонизатор к фреймворку
            JavalinThymeleaf.configure(getTemplateEngine());
        });

        addRoutes(app);

        // Обработчик before запускается перед каждым запросом
        // Устанавливаем атрибут ctx для запросов
        app.before(ctx -> ctx.attribute("ctx", ctx));

        return app;
    }

    // Javalin поддерживает работу с шаблонизатором thymeleaf
    private static TemplateEngine getTemplateEngine() {
        // Создаём инстанс движка шаблонизатора
        TemplateEngine templateEngine = new TemplateEngine();
        // Добавляем к нему диалекты
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());
        // Настраиваем преобразователь шаблонов, так, чтобы обрабатывались
        // шаблоны в директории /templates/
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        // Добавляем преобразователь шаблонов к движку шаблонизатора
        templateEngine.addTemplateResolver(templateResolver);

        return templateEngine;
    }

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        int portNumber = Integer.valueOf(port);
        LOGGER.info("Received port {}", portNumber);
        return portNumber;
    }

    private static String getMode() {
        String mode = System.getenv().getOrDefault("APP_ENV", "development");
        LOGGER.info("Mode: {}", mode);
        return mode;
    }

    private static boolean isProduction() {
        return getMode().equals("production");
    }

    private static void addRoutes(Javalin app) {
        app.get("/", RootController.welcome);

        app.routes(() -> {
            path("urls", () -> {
                get(UrlController.showAllUrls);
                post(UrlController.createUrl);
                path("{id}", () -> {
                    get(UrlController.showUrl);
                    post("/checks", UrlController.checkUrl);
                });
            });
        });
    }

}
