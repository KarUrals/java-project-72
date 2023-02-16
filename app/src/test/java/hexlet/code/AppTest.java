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
    private static MockWebServer mockWebServer;
    private static String mockWebServerUrl;

    private static final String TEST_PAGE_DESCRIPTION = "Test site description";
    private static final String TEST_PAGE_TITLE = "Test site title";
    private static final String TEST_PAGE_H1 = "Hello, World!";
    private static final String NONEXISTENT_URL_ID = "1000000";

    @BeforeAll
    public static void beforeAll() throws IOException {
        // Получаем инстанс приложения
        app = App.getApp();
        // Запускаем приложение на рандомном порту
        app.start(0);
        // Получаем порт, на котором запустилось приложение
        int port = app.port();
        // Формируем базовый URL
        baseUrl = "http://localhost:" + port;

        existingUrl = new QUrl()
                .id.equalTo(1L)
                .findOne();

        mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServerUrl = mockWebServer.url("/").toString();
        String testHtmlPageBody = Files.readString(Paths.get("src/test/resources/simple-doc.html"));
        mockWebServer.enqueue(new MockResponse().setBody(testHtmlPageBody));
    }

    @AfterAll
    public static void afterAll() throws IOException {
        // Останавливаем приложение
        app.stop();
        // Останавливаем mockWebServer
        mockWebServer.shutdown();
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
        void testShowNonexistentUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/" + NONEXISTENT_URL_ID)
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
            assertThat(body).contains("Not found");
        }

        @Test
        void testCreateUrl() {
            String url = "https://www.ya.ru";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asString();

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls")
                    .asString();

            Url actualUrl = new QUrl()
                    .name.equalTo(url)
                    .findOne();

            assertThat(responsePost.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            assertThat(responseGet.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(responseGet.getBody()).contains(url);
            assertThat(responseGet.getBody()).contains("Страница успешно добавлена");

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

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/")
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            assertThat(responseGet.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(responseGet.getBody()).contains("Некорректный URL");
        }

        @Test
        void testCreateExistingUrl() {
            String url = existingUrl.getName();

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asString();

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls")
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            assertThat(responseGet.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(responseGet.getBody()).contains(url);
            assertThat(responseGet.getBody()).contains("Страница уже существует");
        }

        @Test
        void testCheckUrl() {

            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", mockWebServerUrl)
                    .asString();

            Url createdUrl = new QUrl()
                    .name.equalTo(mockWebServerUrl.substring(0, mockWebServerUrl.length() - 1))
                    .findOne();

            HttpResponse<String> responsePost2 = Unirest
                    .post(baseUrl + "/urls/" + createdUrl.getId() + "/checks")
                    .asString();

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls/" + createdUrl.getId())
                    .asString();

            UrlCheck latestCheck = new QUrlCheck()
                    .url.equalTo(createdUrl)
                    .orderBy()
                    .id
                    .desc()
                    .findOne();

            assertThat(responsePost1.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost1.getHeaders().getFirst("Location")).isEqualTo("/urls");

            assertThat(createdUrl).isNotNull();

            assertThat(responsePost2.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost2.getHeaders().getFirst("Location")).isEqualTo("/urls/" + createdUrl.getId());

            assertThat(responseGet.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(responseGet.getBody()).contains(mockWebServerUrl.substring(0, mockWebServerUrl.length() - 1));
            assertThat(responseGet.getBody()).contains("Страница успешно проверена");

            assertThat(latestCheck).isNotNull();
            assertThat(latestCheck.getDescription()).isEqualTo(TEST_PAGE_DESCRIPTION);
            assertThat(latestCheck.getTitle()).isEqualTo(TEST_PAGE_TITLE);
            assertThat(latestCheck.getH1()).isEqualTo(TEST_PAGE_H1);
        }

        @Test
        void testCheckIncorrectUrl() {

            String incorrectUrl = "https://www.example.som";

            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", incorrectUrl)
                    .asString();

            Url createdUrl = new QUrl()
                    .name.equalTo(incorrectUrl)
                    .findOne();

            HttpResponse<String> responsePost2 = Unirest
                    .post(baseUrl + "/urls/" + createdUrl.getId() + "/checks")
                    .asString();

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls/" + createdUrl.getId())
                    .asString();

            UrlCheck latestCheck = new QUrlCheck()
                    .url.equalTo(createdUrl)
                    .orderBy()
                    .id
                    .desc()
                    .findOne();

            assertThat(responsePost2.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
            assertThat(responsePost2.getHeaders().getFirst("Location")).isEqualTo("/urls/" + createdUrl.getId());

            assertThat(responseGet.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(responseGet.getBody()).contains(incorrectUrl);
            assertThat(responseGet.getBody()).contains("Некорректный адрес");

            assertThat(latestCheck).isNull();
        }

        @Test
        void testCheckNonexistentUrl() {
            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls/" + NONEXISTENT_URL_ID + "/checks")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
            assertThat(body).contains("Not found");
        }
    }
}
