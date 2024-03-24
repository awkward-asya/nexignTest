package nexign.task.services.udr;

import java.time.Duration;


public class RecordUDR {
    private String msisdn;
    private CallDetails incomingCall;
    private CallDetails outgoingCall;

    public RecordUDR(String msisdn) {
        this.msisdn = msisdn;
        this.incomingCall = new CallDetails();
        this.outgoingCall = new CallDetails();
    }


    public String getMsisdn() {
        return msisdn;
    }

    public CallDetails getIncomingCall() {
        return incomingCall;
    }

    public CallDetails getOutgoingCall() {
        return outgoingCall;
    }

    public static class CallDetails {
        private long totalTimeSeconds;

        public void addDuration(Duration duration) {
            totalTimeSeconds += duration.getSeconds();
        }

        public String getTotalTime() {
            long seconds = totalTimeSeconds % 60;
            long minutes = (totalTimeSeconds / 60) % 60;
            long hours = (totalTimeSeconds / (60 * 60)) % 24;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
    }


}