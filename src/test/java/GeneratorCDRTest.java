import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Random;

import nexign.task.services.cdr.GeneratorCDR;
import nexign.task.services.cdr.RecordCDR;

@ExtendWith(MockitoExtension.class)
public class GeneratorCDRTest {

    @Test
    public void generateMonthCDR_CreatesFile() throws Exception {
        GeneratorCDR generator = new GeneratorCDR();
        Connection mockConnection = mock(Connection.class);

        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        Method privateMethod = GeneratorCDR.class.getDeclaredMethod("generateMonthCDR", int.class, int.class, int.class, int.class, List.class, List.class, Random.class, Connection.class);
        privateMethod.setAccessible(true);
        File file = (File) privateMethod.invoke(generator, 1, 2024, 50, 3600, List.of("79123456789"), List.of("01", "02"), new Random(), mockConnection);

        assertNotNull(file);
        assertTrue(file.exists());
    }

    @Test
    public void insertTransactionIntoDatabase_insertionSuccess() throws Exception {
        GeneratorCDR generator = new GeneratorCDR();
        RecordCDR record = new RecordCDR("01", "79123456789", 1717245455, 1717248904);

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        Method privateMethod = GeneratorCDR.class.getDeclaredMethod("insertTransactionIntoDatabase", RecordCDR.class, Connection.class);
        privateMethod.setAccessible(true);
        privateMethod.invoke(generator, record, mockConnection);

        verify(mockStatement, times(1)).executeUpdate();
    }
}
