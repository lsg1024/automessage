package excel.automessage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    private String receiver;
    private String content;
    private String status;
    private String errorMessage;

    @ManyToOne
    @JoinColumn(name = "messageStorageId")
    private MessageStorage messageStorage;

    @Builder
    public MessageHistory(String receiver, String content, String status, String errorMessage) {
        this.receiver = receiver;
        this.content = content;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public void setMessageStorage(MessageStorage messageStorage) {
        this.messageStorage = messageStorage;
    }


}
