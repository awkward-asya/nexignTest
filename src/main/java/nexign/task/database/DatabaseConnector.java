package nexign.task.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Класс для подключения к базе данных, загружает параметры подключения из конфигурационного файла.
 */
public class DatabaseConnector {
    private static Connection connection;

    /**
     * Получает соединение с базой данных, используя параметры, указанные в файле database.properties
     *
     * @return объект Connection – соединение с базой данных
     *
     * @throws RuntimeException в случае ошибки ввода-вывода или при неудачной попытке установить соединение с базой данных
     */
    public static Connection getConnection() {
        if (connection == null) {
            try {
                Properties properties = new Properties();
                InputStream inputStream = DatabaseConnector.class.getClassLoader().getResourceAsStream("database.properties");
                properties.load(inputStream);

                String url = properties.getProperty("database.url");
                String username = properties.getProperty("database.username");
                String password = properties.getProperty("database.password");

                connection = DriverManager.getConnection(url, username, password);

                inputStream.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Не удалось установить соединение с базой данных.");
            }
        }
        return connection;
    }
}
