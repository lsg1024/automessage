package excel.automessage.controller;

import excel.automessage.dto.MessageDTO;
import excel.automessage.dto.ResponseDTO;
import excel.automessage.service.SmsLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class SmsLogController {

    private final SmsLogService smsLogService;

    @PostMapping("/sms/save")
    public ResponseEntity<?> saveSms(@RequestBody MessageDTO.MessageLog messageLogDto) {

        smsLogService.saveSmsLog();

        return ResponseEntity.ok().body(new ResponseDTO("ok"));
    }


}
