package nexign.task.services.udr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.*;

import nexign.task.utilities.UtilFunctions;

/**
 GeneratorUDR - класс для сервиса, создающего UDR отчеты (Usage Detail Record)
 на основе CDR файлов  (Call Detail Record).
 Обрабатывает CDR файлы для извлечения деталей звонков и создает отчеты по абонентам.
 */
public class GeneratorUDR {

    private List<File> cdrFiles;
    private List<String> subscribers;
    private final String reportDirectory;

    /**
     * Конструктор объекта GeneratorUDR
     * При создании сохраняет список CDR файлов, список всех абонентов, по которым будут создаваться отчеты
     * и директорию, в которой будут сохранены отчеты.
     *
     * @param cdrFiles список CDR файлов с записями о деталях вызовов
     * @param subscribers список телефонных номеров всех абонентов
     * @param directory директория для сохранения отчетов
     */
    public GeneratorUDR(List<File> cdrFiles, List<String> subscribers, String directory) {
        this.cdrFiles = cdrFiles;
        this.subscribers = subscribers;
        this.reportDirectory = directory;
    }

    /**
     * Генерирует отчеты UDR для всех абонентов за весь период тарификации (12 месяцев)
     * на основе предоставленных файлов CDR.
     * Отчеты сохраняются в формате JSON в указанной директории.
     */
    public void generateReport() {
        File directory = new File(reportDirectory);
        UtilFunctions.updateReportDirectory(directory);

        List < TreeMap<String, RecordUDR> > udrMaps = new ArrayList<>();

        for (int i = 0; i < cdrFiles.size(); i++) {
            File cdrFile = cdrFiles.get(i);
            TreeMap<String, RecordUDR> udrMapThisMonth = processCDRFile(cdrFile, i + 1, null, directory);
            udrMaps.add(udrMapThisMonth);
        }

        for (String subscriber : subscribers) {
            List<RecordUDR> recordsForSubscriber = udrMaps.stream()
                    .map(udrMap -> udrMap.getOrDefault(subscriber, null))
                    .filter(Objects::nonNull)
                    .toList();

            printReport(subscriber, recordsForSubscriber, null);
        }
        System.out.println();
    }

    /**
     * Генерирует UDR отчет для заданного абонента за весь период тарификации (12 месяцев).
     * Отчет сохраняется в формате JSON в указанной директории.
     *
     * @param msisdn телефонный номер абонента
     */
    public void generateReport(String msisdn) {
        if (!subscribers.contains(msisdn)) {
            System.err.println("Абонент с номером " + msisdn + " не найден.");
            return;
        }

        File directory = new File(reportDirectory);
        UtilFunctions.updateReportDirectory(directory);

        List <RecordUDR> udrs = new ArrayList<>();

        for (int i = 0; i < cdrFiles.size(); i++) {
            File cdrFile = cdrFiles.get(i);
            RecordUDR udrThisMonth = processCDRFile(cdrFile, i + 1, msisdn, directory).get(msisdn);
            udrs.add(udrThisMonth);
        }

        printReport(msisdn, udrs, null);
    }

    /**
     * Генерирует UDR отчет для заданного абонента на указанный месяц.
     * Отчет сохраняется в формате JSON в указанной директории.
     *
     * @param msisdn телефонный номер абонента
     * @param month  месяц, для которого генерируется отчет (1-12)
     */
    public void generateReport(String msisdn, int month) {

        if (!subscribers.contains(msisdn)) {
            System.err.println("Абонент с номером " + msisdn + " не найден.");
            return;
        }

        if (month < 1 || month > 12) {
            System.err.println("Недопустимый номер месяца");
            return;
        }

        if (month > cdrFiles.size()) {
            System.err.println("Недостаточно файлов для указанного месяца");
            return;
        }

        File directory = new File(reportDirectory);
        UtilFunctions.updateReportDirectory(directory);

        File cdrFile = cdrFiles.get(month - 1);

        RecordUDR udrData = processCDRFile(cdrFile, month, msisdn, directory).get(msisdn);
        List<RecordUDR> udrs = Collections.singletonList(udrData);

        printReport(msisdn, udrs, month);
    }


    /**
     * Обрабатывает CDR файл (Call Detail Record) для извлечения деталей звонков за указанный месяц
     * и создает отображение UDR (Usage Detail Record), по ключу-номеру хранятся все UDR-объект для абонента.
     *
     * @param cdrFile файл CDR, который нужно обработать
     * @param month месяц, для которого создается отчет
     * @param msisdn номер абонента, для которого нужно создать отчет (если null, обрабатываются все абоненты)
     * @param directory  директория, в которой будут сохранены отчеты
     *
     * @return отображение UDR (TreeMap<String, RecordUDR>), хранит детали звонков для каждого абонента.
     * Выбран TreeMap для поддержки упорядочивания ключей для дальнейшего вывода отчетов
     */
    private TreeMap<String, RecordUDR> processCDRFile(File cdrFile, int month, String msisdn, File directory) {
        TreeMap<String, RecordUDR> udrMap = new TreeMap<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(cdrFile));
            String line;

            while ((line = br.readLine()) != null) {
                String[] dataParts = line.split(", ");

                String callType = dataParts[0];
                String phoneNumber = dataParts[1];

                // проверяем, нужно ли обработать строку из данных для конкретного абонента
                if (msisdn == null || msisdn.equals(phoneNumber)) {
                    Temporal startTime = Instant.ofEpochSecond(Long.parseLong(dataParts[2]));
                    Temporal endTime = Instant.ofEpochSecond(Long.parseLong(dataParts[3]));

                    // вычисляем продолжительность вызова
                    Duration callDuration = Duration.between(startTime, endTime);

                    // создаем объект UDR записи по текущему абоненту и месяцу
                    RecordUDR udr = udrMap.getOrDefault(phoneNumber, new RecordUDR(phoneNumber));

                    // обновляем информацию о длительности вызова этого типа в объекте UDR
                    if (callType.equals("01")) {
                        udr.getOutgoingCall().addDuration(callDuration);
                    } else if (callType.equals("02")) {
                        udr.getIncomingCall().addDuration(callDuration);
                    }
                    udrMap.put(phoneNumber, udr);

                }
            }
            br.close();


            Set<String> udrKeys = udrMap.keySet();

            if (msisdn == null) {
                // добавляем пустые UDR-записи для номеров, которые не встретились в исходных данных
                for (String phoneNumber : subscribers) {
                    if (!udrKeys.contains(phoneNumber)) {
                        udrMap.put(phoneNumber, new RecordUDR(phoneNumber));
                    }
                }
            } else {
                if (!udrKeys.contains(msisdn)) {
                    // если вызвана перегрузка по конкретному абоненту, добавляем только его
                    udrMap.put(msisdn, new RecordUDR(msisdn));
                }
            }

            // сохраняем результат для каждого номера в отдельный JSON-файл
            for (String phoneNumber : udrMap.keySet()) {
                RecordUDR udr = udrMap.get(phoneNumber);
                saveUDRToJson(directory, udr, phoneNumber, month);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return udrMap;
    }

    /**
     * Преобразует объект RecordUDR (Usage Detail Record) в формате JSON и сохраняет в указанной директории.
     *
     * @param directory директория, в которой будет сохранен файл
     * @param udr объект UDR для сохранения
     * @param phoneNumber номер телефона абонента, для которого создается отчет
     * @param month месяц, за который создается отчет
     */
    private void saveUDRToJson(File directory, RecordUDR udr, String phoneNumber, int month) {
        String filename = phoneNumber + "_" + month + ".json";

        File reportFile = new File(directory, filename);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile))) {
            objectMapper.writeValue(writer, udr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Выводит отчет по всем UDR-файлам для конкретного абонента.
     *
     * @param phoneNumber телефонный номер абонента
     * @param udrs список UDR-файлов по абоненту
     */
    private void printReport(String phoneNumber, List<RecordUDR> udrs, Integer month) {
        System.out.println("Отчет по абоненту " + phoneNumber);
        System.out.println("-----------------------------------------------");
        System.out.println("| Абонент     | Месяц  | Исходящие | Входящие |");
        System.out.println("-----------------------------------------------");

        if (month != null) {
            RecordUDR monthUDR = udrs.get(0);
            printReportRow(phoneNumber, month, monthUDR);
        } else {
            for (month = 1; month <= udrs.size(); month++) {
                RecordUDR monthUDR = udrs.get(month - 1);
                printReportRow(phoneNumber, month, monthUDR);
            }
        }

        System.out.println("-----------------------------------------------\n");
    }

    private void printReportRow(String phoneNumber, int month, RecordUDR monthUDR) {
        String formattedMonth = String.format("%02d", month);
        String outgoingCallTime = monthUDR.getOutgoingCall().getTotalTime();
        String incomingCallTime = monthUDR.getIncomingCall().getTotalTime();

        System.out.printf("| %-10s | %2s%-4s | %9s | %8s |%n",
                phoneNumber, "", formattedMonth, outgoingCallTime, incomingCallTime);
    }

}