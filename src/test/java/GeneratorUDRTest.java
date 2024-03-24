import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nexign.task.services.udr.GeneratorUDR;
import nexign.task.services.udr.RecordUDR;

public class GeneratorUDRTest {

    private static String testCdrDir;
    private static String testUdrDir;

    @BeforeAll
    public static void setUp() throws IOException {
        testCdrDir = "test_cdr_files";
        testUdrDir = "test_udr_files";

        Files.createDirectories(Path.of(testCdrDir));
        Files.createDirectories(Path.of(testUdrDir));

        // Создаем тестовые файлы CDR
        createTestCDRFiles();
    }

    private static void createTestCDRFiles() throws IOException {
        Random random = new Random();

        for (int i = 1; i <= 12; i++) {
            File cdrFile = new File(testCdrDir, "cdr_" + i + ".txt");

            cdrFile.createNewFile();

            FileWriter writer = new FileWriter(cdrFile);

            for (int j = 0; j < 10; j++) {
                String cdrRecord;
                if (random.nextBoolean()) {
                    cdrRecord = "02, 79876543221, 1709798657, 1709799601\n";
                } else {
                    cdrRecord = "01, 79996667755, 1709899870, 1709905806\n";
                }
                writer.write(cdrRecord);
            }

            writer.close();
        }
    }

    @Test
    public void printReport_Test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        GeneratorUDR generatorUDR = new GeneratorUDR(new ArrayList<>(), new ArrayList<>(), "testDirectory");
        List<RecordUDR> testRecordUDRs = new ArrayList<>();

        int separators = 3;
        int headings = 2;
        int newLines = 1;

        int udrLines = 3;
        for (int i = 0; i < udrLines; i++) {
            testRecordUDRs.add(new RecordUDR("79123456789"));
        }

        Method privateMethod = GeneratorUDR.class.getDeclaredMethod("printReport", String.class, List.class);
        privateMethod.setAccessible(true);

        File outputFile = File.createTempFile("output", ".txt");
        PrintStream originalOut = System.out;

        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        PrintStream printStream = new PrintStream(fileOutputStream);
        System.setOut(printStream);

        privateMethod.invoke(generatorUDR, "79123456789", testRecordUDRs);

        System.setOut(originalOut);

        int numberOfLines = countLines(outputFile);
        assertEquals(separators + headings + udrLines + newLines, numberOfLines);
    }

    private int countLines(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        int lines = 0;
        String line;
        while ((line = reader.readLine()) != null) {lines++;
            System.out.println("Строка " + lines + ": " + line);
        };
        System.out.println();
        reader.close();
        return lines;
    }

    @Test
    public void generateReport_Test() {
        List<File> cdrFiles = getFilesFromDirectory(testCdrDir);
        List<String> subscribers = List.of("79876543221", "79996667755");

        GeneratorUDR generatorUDR = new GeneratorUDR(cdrFiles, subscribers, testUdrDir);
        generatorUDR.generateReport();

        File udrDir = new File(testUdrDir);

        // Проверяем наличие 12*число_абонентов JSON-файлов
        File[] udrFiles = udrDir.listFiles();
        assertNotNull(udrFiles);
        assertEquals(12 * subscribers.size(), udrFiles.length);

        for (File udrFile : udrFiles) {
            assertTrue(udrFile.getName().endsWith(".json"));
        }

    }

    private List<File> getFilesFromDirectory(String directoryPath) {
        List<File> files = new ArrayList<>();
        File directory = new File(directoryPath);
        if (directory.isDirectory()) {
            File[] fileList = directory.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile()) {
                        files.add(file);
                    }
                }
            }
        }
        return files;
    }

    @AfterAll
    public static void clearUp() throws IOException {
        deleteDirectory(new File(testCdrDir));
        deleteDirectory(new File(testUdrDir));
    }

    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) { deleteDirectory(file);}
                    else { file.delete(); }
                }
            }
        }
        directory.delete();
    }
}
