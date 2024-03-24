package nexign.task;

import java.sql.Connection;

import nexign.task.database.DatabaseConnector;
import nexign.task.database.DatabaseInitializer;

import nexign.task.services.cdr.GeneratorCDR;
import nexign.task.services.udr.GeneratorUDR;

import nexign.task.utilities.UtilFunctions;


public class Main {
    public static void main(String[] args) {
        try (Connection connection = DatabaseConnector.getConnection()) {
            if (connection != null) {
                // если подключение установлено успешно, выполняем генерацию CDR и UDR

                int subscribersMinCount = 10;

                // если в базе нет таблицы Subscribers, генирируем номера телефонов и добавляем их в базу
                DatabaseInitializer.initializeSubscribers(connection, subscribersMinCount);

                // обновляем таблицу с CDR в базе, для того чтобы там гарантированно были
                // только новые сгенерированные CDR-записи
                DatabaseInitializer.createCDRTable(connection);

                final int year = 2024;
                String reportsDirectory = "reports";

                GeneratorCDR generatorCDR = new GeneratorCDR();
                // сразу передаем резуьтат генерации CDR файлов в конструктор для объекта GeneratorUDR
                GeneratorUDR generatorUDR = new GeneratorUDR(
                        generatorCDR.generateCDRs(connection, year),
                        UtilFunctions.getAllSubscribers(connection),
                        reportsDirectory);

                // проверяем количество переданных аргументов командной строки и вызываем соответствующие функции
                if (args.length > 2) {
                    System.out.println("Ожидаемые аргументы: [msisdn] [month]");
                } else {
                    switch (args.length) {
                        case 0: //  по умолчанию (без аргументов) вызывается generatorUDR.generateReport()
                            generatorUDR.generateReport();
                            break;
                        case 1:
                            if (UtilFunctions.isValidMobilePhoneNumber(args[0])) {
                                generatorUDR.generateReport(args[0]);
                            } else {
                                System.out.println("Неверный формат для номера телефона.");
                            }
                            break;
                        case 2:
                            if (UtilFunctions.isValidMobilePhoneNumber(args[0]) && UtilFunctions.isValidMonth(args[1])) {
                                generatorUDR.generateReport(args[0], Integer.parseInt(args[1]));
                            } else {
                                System.out.println("Неверный формат для номера телефона или месяца.");
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}