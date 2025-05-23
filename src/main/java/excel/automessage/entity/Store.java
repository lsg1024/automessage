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
@SQLDelete(sql = "UPDATE store SET deleted = true where store_id = ?")
@SQLRestriction("deleted = false")
public class Store {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long storeId;
    @Column(unique = true)
    private String storeName;

    @Column(unique = true)
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
    public void setStoreNameAndPhoneNumber(String storeName, String storePhoneNumber) {

        if (storePhoneNumber != null && storeName != null) {
            this.storePhoneNumber = storePhoneNumber;
            this.storeName = storeName;
        }
    }
}
