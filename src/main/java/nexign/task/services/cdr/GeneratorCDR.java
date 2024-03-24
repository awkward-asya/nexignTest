package nexign.task.services.cdr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.util.*;

import nexign.task.utilities.UtilFunctions;

/**
 * GeneratorCDR - класс для сервиса, генерирующего CDR файлы (Call Detail Record).
 * Создает CDR файлы с помощью случайно сгенерированных записей о звонках для каждого месяца в указанном году.
 *
 */
public class GeneratorCDR {
    /**
     * activeCalls - хранит активные звонки в виде отображения между номером телефона и временем завершения звонка.
     * Добавлен для предотвращения ситуаций, когда абонент совершает одновременно 2 звонка
     */
    private Map<String, Long> activeCalls = new HashMap<>();

    /**
     * Генерирует CDR файлы для всех месяцев в указанном году
     * на основе случайно сгенерированных данных о звонках.
     *
     * @param connection соединение с базой данных для сохранения записей о звонках
     * @param year год, за который генерируются CDR файлы
     *
     * @return список сгенерированных файлов CDR
     */
    public List<File> generateCDRs(Connection connection, int year){
        Random random = new Random();
        List<File> reportsCDR = new ArrayList<>();

        final int monthsInYear = 12;

        final int callsMinCount = 20;
        final int callsMaxCount = 1000;
        final int callsMaxDuration = 3600;

        List<String> subscribers = UtilFunctions.getAllSubscribers(connection);
        List<String> callTypes = List.of("01", "02");

        for (int month = 1; month <= monthsInYear; month++) {
            int callsCount = UtilFunctions.getIntegerInRange(callsMinCount, callsMaxCount);
            File monthCDR = generateMonthCDR(month, year, callsCount,
                    callsMaxDuration, subscribers, callTypes, random, connection);
            reportsCDR.add(monthCDR);
        }

        return reportsCDR;
    }

    /**
     * Генерирует CDR файл для конкретного месяца указанного года на основе случайных данных о звонках.
     *
     * @param month номер месяца
     * @param year год
     * @param callsCount количество звонков для генерации
     * @param callsMaxDuration максимальная продолжительность звонка (в секундах)
     * @param subscribers список телефонных номеров абонентов
     * @param callTypes список типов звонков
     * @param random генератор случайных чисел
     * @param connection соединение с базой данных для сохранения записей о звонках
     *
     * @return файл CDR для указанного месяца
     */
    private File generateMonthCDR(int month, int year, int callsCount, int callsMaxDuration,
                                             List<String> subscribers, List<String> callTypes,
                                             Random random, Connection connection){
        // очищаем список активных звонков с прошлого месяца
        activeCalls.clear();
        List<RecordCDR> monthCDR = new ArrayList<>();

        File directory = new File("cdr_files");
        if (!directory.exists()) { directory.mkdir(); }

        File file = new File(directory, "cdr_" + month + ".txt");


        try (FileWriter writer = new FileWriter(file)) {
            // определяем начало и конец месяца в Unix time
            LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

            long startUnixTime = startOfMonth.toInstant(ZoneOffset.UTC).getEpochSecond();
            long endUnixTime = endOfMonth.toInstant(ZoneOffset.UTC).getEpochSecond();
            long unixTimeRange = endUnixTime - startUnixTime;

            // генерируем записи о звонках до нужного количества
            for (int i = 0; i <= callsCount; i++) {
                String callingPhoneNumber = subscribers.get(random.nextInt(subscribers.size()));
                String callType = callTypes.get(random.nextInt(2));

                long startTime = startUnixTime + random.nextInt((int) unixTimeRange);
                long endTime = startTime + random.nextInt(callsMaxDuration) + 1;

                // проверяем, не находится ли абонент в активном звонке в это время
                if (activeCalls.containsKey(callingPhoneNumber)) {
                    long activeCallEndTime = activeCalls.get(callingPhoneNumber);
                    if (startTime < activeCallEndTime) {
                        // если абонент во время нового сгенерированного звонка уже говорит с кем-то - пропускаем этот звонок
                        continue;
                    }
                }

                RecordCDR record = new RecordCDR(callType, callingPhoneNumber, startTime, endTime);
                monthCDR.add(record);
                activeCalls.put(callingPhoneNumber, endTime);
            }

            // сортируем (по времени начала звонка), чтобы сгенерированные записи шли в CDR файле в хронологическом порядке
            Collections.sort(monthCDR);

            // записываем каждую запись CDR в файл и вставляем её в базу данных
            for (RecordCDR record : monthCDR) {
                writer.write(record + "\n");
                insertTransactionIntoDatabase(record, connection);
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * Вставляет запись о звонке в базу данных.
     *
     * @param record запись о звонке (RecordCDR)
     * @param connection соединение с базой данных
     *
     * @throws SQLException если произошла ошибка при выполнении SQL-запроса
     */
    private void insertTransactionIntoDatabase(RecordCDR record, Connection connection) throws SQLException {
        String sql = "insert into cdr (call_type, msisdn, start_time_unix, end_time_unix) " +
                "values (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.callType());
            statement.setString(2, record.phoneNumber());
            statement.setLong(3, record.startTimeUnix());
            statement.setLong(4, record.endTimeUnix());

            statement.executeUpdate();
        }
    }

}
