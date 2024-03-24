import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import nexign.task.utilities.UtilFunctions;

class UtilFunctionsTest {

    @Mock
    Connection mockConnection;

    @Mock
    PreparedStatement mockStatement;

    @Mock
    ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @Test
    void isValidPhoneNumber_Test() {
        assertTrue(UtilFunctions.isValidMobilePhoneNumber("79123456789"));

        assertFalse(UtilFunctions.isValidMobilePhoneNumber("71123456789"));
        assertFalse(UtilFunctions.isValidMobilePhoneNumber("123456789"));
        assertFalse(UtilFunctions.isValidMobilePhoneNumber("abacaba"));
        assertFalse(UtilFunctions.isValidMobilePhoneNumber(null));
    }

    @Test
    void isValidMonth_Test() {
        assertTrue(UtilFunctions.isValidMonth("1"));
        assertTrue(UtilFunctions.isValidMonth("01"));
        assertTrue(UtilFunctions.isValidMonth("00001"));

        assertFalse(UtilFunctions.isValidMonth("-15"));
        assertFalse(UtilFunctions.isValidMonth("1024"));
        assertFalse(UtilFunctions.isValidMonth(null));
    }

    @Test
    void getIntegerInRange_ReturnsNumberInRange() {
        int min = 1;
        int max = 10;
        int result = UtilFunctions.getIntegerInRange(min, max);
        assertTrue(result >= min && result <= max);

        assertThrows(IllegalArgumentException.class, () -> {
            UtilFunctions.getIntegerInRange(max, min);
        });
    }

    @Test
    void updateReportDirectory_ifDirectoryExists_DeletesFiles() {
        File mockDirectory = mock(File.class);
        when(mockDirectory.exists()).thenReturn(true);
        File[] mockFiles = new File[2];
        mockFiles[0] = mock(File.class);
        mockFiles[1] = mock(File.class);
        when(mockDirectory.listFiles()).thenReturn(mockFiles);

        UtilFunctions.updateReportDirectory(mockDirectory);

        verify(mockDirectory, never()).mkdirs();
        verify(mockFiles[0]).delete();
        verify(mockFiles[1]).delete();
    }

    @Test
    void updateReportDirectory_ifDirectoryDoesNotExist_CreatesDirectory() {
        File mockDirectory = mock(File.class);
        when(mockDirectory.exists()).thenReturn(false);

        UtilFunctions.updateReportDirectory(mockDirectory);

        verify(mockDirectory).mkdirs();
        verify(mockDirectory, never()).listFiles();
    }

    @Test
    void getAllSubscribers_ReturnsListOfSubscribers() throws SQLException {
        List<String> expectedSubscribers = List.of("79876543221", "79996667755");
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("MSISDN")).thenReturn("79876543221", "79996667755");

        List<String> actualSubscribers = UtilFunctions.getAllSubscribers(mockConnection);

        assertEquals(expectedSubscribers, actualSubscribers);
    }
}
