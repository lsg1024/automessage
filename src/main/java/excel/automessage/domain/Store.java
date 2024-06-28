package excel.automessage.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storeId")
    Long storeId;
    @Column(name = "storeName", unique = true)
    String storeName;
    String storePhoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historyId")
    private MessageHistory messageHistory;

    @Builder
    public Store(String storeName, String storePhoneNumber) {
        this.storeName = storeName;
        this.storePhoneNumber = storePhoneNumber;
    }

    public void setStorePhoneNumber(String storePhoneNumber) {

        if (storePhoneNumber != null) {
            this.storePhoneNumber = storePhoneNumber;
        }
    }
}
