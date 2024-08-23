package excel.automessage.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historyId")
    private MessageHistory messageHistory;

    @Builder
    public ProductHistory(String productName, MessageHistory messageHistory) {
        this.productName = productName;
        this.messageHistory = messageHistory;
    }

}
