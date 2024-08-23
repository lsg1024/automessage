package excel.automessage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection
    @CollectionTable(name = "productNames", joinColumns = @JoinColumn(name = "history_id"))
    @Column(name = "productName")
    private List<String> productNames = new ArrayList<>();

    @Builder
    public MessageHistory(String receiver, String content, String status, String errorMessage, List<String> productNames) {
        this.receiver = receiver;
        this.content = content;
        this.status = status;
        this.errorMessage = errorMessage;
        this.productNames = productNames;
    }

    public void setMessageStorage(MessageStorage messageStorage) {
        this.messageStorage = messageStorage;
    }

    public void addSaleItem(String productName) {
        this.productNames.add(productName);
    }

}
