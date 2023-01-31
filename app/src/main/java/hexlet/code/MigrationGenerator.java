package hexlet.code;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;

import java.io.IOException;

public class MigrationGenerator {
    public static void main(String[] args) throws IOException {
        DbMigration dbMigration = DbMigration.create(); // Создаём миграцию

        dbMigration.addPlatform(Platform.H2, "h2"); // Указываем платформу, в этом случае H2

        dbMigration.generateMigration(); // Генерируем миграцию
    }
}
