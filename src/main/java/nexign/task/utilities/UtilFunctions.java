package nexign.task.utilities;

import java.io.File;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class UtilFunctions {

    public static boolean isValidMobilePhoneNumber(String phoneNumber) {
        try {
            return Pattern.matches("79\\d{9}", phoneNumber);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidMonth(String month) {
        try {
            int monthNumber = Integer.parseInt(month);
            return (monthNumber >= 1 && monthNumber <= 12);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int getIntegerInRange(int min, int max) throws IllegalArgumentException {
        if (min >= max) {
            throw new IllegalArgumentException("Минимальное значение должно быть меньше максимального значения");
        }

        Random random = new Random();
        int range = max - min + 1;
        return random.nextInt(range) + min;
    }

    public static String generateRandomDigits(int length) {
        StringBuilder digits = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }

    public static void updateReportDirectory(File directory){
        if (!directory.exists()) {
            directory.mkdirs();
        } else {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    public static List<String> getAllSubscribers(Connection connection) {
        List<String> phoneNumbers = new ArrayList<>();

        try {
            String sql = "select msisdn from Subscribers";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String phoneNumber = resultSet.getString("MSISDN");
                        phoneNumbers.add(phoneNumber);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return phoneNumbers;
    }
}