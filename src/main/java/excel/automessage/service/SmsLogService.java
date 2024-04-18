package excel.automessage.service;

import excel.automessage.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsLogService {

    public final SmsLogRepository smsLogRepository;

    public void saveSmsLog() {

    }
}
