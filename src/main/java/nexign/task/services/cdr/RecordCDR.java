package nexign.task.services.cdr;

public record RecordCDR(String callType, String phoneNumber, long startTimeUnix, long endTimeUnix)
        implements Comparable<RecordCDR> {

    @Override
    public int compareTo(RecordCDR other) {
        // Сравнение CDR записей по времени начала звонка
        return Long.compare(this.startTimeUnix, other.startTimeUnix);
    }

    @Override
    public String toString() {
        return  callType + ", " +
                phoneNumber + ", " +
                startTimeUnix + ", " +
                endTimeUnix;
    }
}
