package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;

import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Transaction;

import io.javalin.Javalin;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static Url existingUrl;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() {
        // Получаем инстанс приложения
        app = App.getApp();
        // Запускаем приложение на рандомном порту
        app.start(0);
        // Получаем порт, на котором запустилось приложение
        int port = app.port();
        // Формируем базовый URL
        baseUrl = "http://localhost:" + port;

//        database = DB.getDefault();

        existingUrl = new QUrl()
                .id.equalTo(1L)
                .findOne();
    }

    @AfterAll
    public static void afterAll() {
        // Останавливаем приложение
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Nested
    class RootTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl)
                    .asString();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(response.getBody()).contains("Анализатор страниц");
            assertThat(response.getBody()).contains("Пример: https://www.example.com");
            assertThat(response.getBody()).contains("KarUrals");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains(existingUrl.getName());
        }

        @Test
        void testShowUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/" + existingUrl.getId())
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains(existingUrl.getName());
        }

        @Test
        void testCreateUrl() {
            String url = "https://www.ya.ru";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains(url);
            assertThat(body).contains("Страница успешно добавлена");

            Url actualUrl = new QUrl()
                    .name.equalTo(url)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(url);
        }

        @Test
        void testCreateInvalidUrl() {
            String url = "invalidUrl";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains("Некорректный URL");
        }

        @Test
        void testCreateExistingUrl() {
            String url = existingUrl.getName();
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains(url);
            assertThat(body).contains("Страница уже существует");
        }

        @Test
        void testCheckUrl() throws IOException {
            String description = "Test site description";
            String title = "Test site title";
            String h1 = "Hello, World!";

            MockWebServer mockWebServer = new MockWebServer();
            mockWebServer.start();

            String mockWebServerUrl = mockWebServer.url("/").toString();

            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", mockWebServerUrl)
                    .asString();

            assertThat(responsePost1.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost1.getHeaders().getFirst("Location")).isEqualTo("/urls");

            Url createdUrl = new QUrl()
                    .name.equalTo(mockWebServerUrl.substring(0, mockWebServerUrl.length() - 1))
                    .findOne();

            assertThat(createdUrl).isNotNull();

            String testHtmlBody = Files.readString(Paths.get("src/test/resources/simple-doc.html"));
            mockWebServer.enqueue(new MockResponse().setBody(testHtmlBody));

            HttpResponse<String> responsePost2 = Unirest
                    .post(baseUrl + "/urls/" + createdUrl.getId() + "/checks")
                    .asString();

            assertThat(responsePost2.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost2.getHeaders().getFirst("Location")).isEqualTo("/urls/" + createdUrl.getId());

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/" + createdUrl.getId())
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains(mockWebServerUrl.substring(0, mockWebServerUrl.length() - 1));
            assertThat(body).contains("Страница успешно проверена");

            UrlCheck latestCheck = new QUrlCheck()
                    .url.equalTo(createdUrl)
                    .orderBy()
                        .id
                        .desc()
                    .findOne();

            assertThat(latestCheck).isNotNull();
            assertThat(latestCheck.getDescription()).isEqualTo(description);
            assertThat(latestCheck.getTitle()).isEqualTo(title);
            assertThat(latestCheck.getH1()).isEqualTo(h1);

            mockWebServer.shutdown();
        }
    }
}
