package excel.automessage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE khanMessage.store SET deleted = true where store_id = ?")
@SQLRestriction("deleted = false")
public class Store {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storeId")
    private Long storeId;
    @Column(name = "storeName")
    private String storeName;
    private String storePhoneNumber;
    private boolean deleted = Boolean.FALSE;

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
