package excel.automessage.entity;

import excel.automessage.entity.auditing.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageStorage extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageStorageId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "messageStorage")
    private List<MessageHistory> messageHistories;

    @Builder
    public MessageStorage(List<MessageHistory> messageHistories) {
        this.messageHistories = messageHistories;
    }

    public void addMessageHistory(MessageHistory messageHistory) {
        this.messageHistories.add(messageHistory);
        messageHistory.setMessageStorage(this);
    }

}
