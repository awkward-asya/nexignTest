package nexign.task.database;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import nexign.task.utilities.UtilFunctions;

/**
 * Класс для инициализации базы данных.
 */
public class DatabaseInitializer {

    /**
     * Инициализирует абонентов в базе данных, если таблица абонентов не существует.
     * Генерирует и вставляет номера телефонов абонентов в базу данных.
     *
     * @param connection соединение с базой данных
     * @param subscribersMinCount минимальное количество абонентов для инициализации
     */
    public static void initializeSubscribers(Connection connection, int subscribersMinCount) {
        if (! ifSubscribersTableExists(connection)) {
            createSubscribersTable(connection);
            int subscribersMaxCount = subscribersMinCount * 10;

            Set<String> phoneNumbers = generatePhoneNumbers(subscribersMinCount, subscribersMaxCount);
            insertSubscribers(connection, phoneNumbers);
        }
    }

    /**
     * Проверяет, существует ли таблица абонентов в базе данных.
     *
     * @param connection соединение с базой данных
     *
     * @return true, если таблица абонентов существует, в противном случае - false
     */
    private static boolean ifSubscribersTableExists(Connection connection) {
        boolean tableExists = false;

        try {
            ResultSet resultSet = connection.getMetaData().getTables(null, null, "SUBSCRIBERS", null);
            tableExists = resultSet.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tableExists;
    }

    /**
     * Создаёт таблицу абонентов в базе данных.
     *
     * @param connection соединение с базой данных
     */
    private static void createSubscribersTable(Connection connection) {
        try {
            String sql = "create table Subscribers (msisdn VARCHAR(11) PRIMARY KEY)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Генерирует набор номеров телефонов в заданном диапазоне.
     *
     * @param minCount минимальное количество номеров телефонов для генерации
     * @param maxCount максимальное количество номеров телефонов для генерации
     *
     * @return множество сгенерированных номеров телефонов
     */
    private static Set<String> generatePhoneNumbers(int minCount, int maxCount) {
        // выбран set, чтобы гарантированно не было дублирующихся номеров
        Set<String> phoneNumbers = new HashSet<>();
        try {
            int count = UtilFunctions.getIntegerInRange(minCount, maxCount);

            while (phoneNumbers.size() < count) {
                String phoneNumber = "79" + UtilFunctions.generateRandomDigits(9);
                phoneNumbers.add(phoneNumber);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return phoneNumbers;
    }

    /**
     * Вставляет номера телефонов абонентов в таблицу абонентов.
     *
     * @param connection соединение с базой данных
     * @param phoneNumbers набор номеров телефонов для вставки
     */
    private static void insertSubscribers(Connection connection, Set<String> phoneNumbers) {
        try {
            String sql = "insert into Subscribers (msisdn) values (?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (String phoneNumber : phoneNumbers) {
                    statement.setString(1, phoneNumber);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создаёт таблицу для CDR записей (записей о деталях вызова) в базе данных.
     *
     * @param connection соединение с базой данных
     */
    public static void createCDRTable(Connection connection) {
        try {
            String dropSql = "drop table if exists CDR";
            try (PreparedStatement dropStatement = connection.prepareStatement(dropSql)) {
                dropStatement.executeUpdate();
            }

            String createSql = "create table CDR (" +
                    "call_type VARCHAR(2), " +
                    "msisdn VARCHAR(11), " +
                    "start_time_unix BIGINT, " +
                    "end_time_unix BIGINT, " +
                    "PRIMARY KEY (msisdn, start_time_unix), " +
                    "FOREIGN KEY (msisdn) REFERENCES Subscribers (msisdn)" +
                    ")";

            try (PreparedStatement createStatement = connection.prepareStatement(createSql)) {
                createStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
