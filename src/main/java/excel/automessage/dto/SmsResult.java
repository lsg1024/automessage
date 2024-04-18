package excel.automessage.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class SmsResult {
    private String statusCode;
    private String statusName;
    private List<Message> messages;
    private int pageIndex;
    private int pageSize;
    private int itemCount;
    private boolean hasMore;

    @Getter @Setter
    public static class Message {
        private String requestId;
        private String campaignId;
        private String messageId;
        private String requestTime;
        private String contentType;
        private String type;
        private String countryCode;
        private String from;
        private String to;
        private String completeTime;
        private String telcoCode;
        private String status;
        private String statusMessage;
    }
}
