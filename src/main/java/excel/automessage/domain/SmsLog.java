package excel.automessage.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "smsLogId")
    Long smsLogId;
    String messageId;
    String requestTime;
    String completeTime;
    String smsFrom;
    String smsTo;
    String smsContent;

}
