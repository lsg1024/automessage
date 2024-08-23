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
    private String storeName;

    @ManyToOne
    @JoinColumn(name = "messageStorageId")
    private MessageStorage messageStorage;

    @OneToMany(mappedBy = "messageHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductHistory> productHistories = new ArrayList<>();

    @Builder
    public MessageHistory(String receiver, String content, String status, String errorMessage, String storeName, List<ProductHistory> productNames) {
        this.receiver = receiver;
        this.content = content;
        this.status = status;
        this.errorMessage = errorMessage;
        this.storeName = storeName;
        if (productNames != null) {
            this.productHistories.addAll(productNames);
            for (ProductHistory productHistory : productNames) {
                productHistory.setMessageHistory(this);
            }
        }
    }

    public void setMessageStorage(MessageStorage messageStorage) {
        this.messageStorage = messageStorage;
    }

}
