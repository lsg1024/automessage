package excel.automessage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import excel.automessage.dto.MessageDTO;
import excel.automessage.dto.ResponseDTO;
import excel.automessage.dto.SmsResponseDTO;
import excel.automessage.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;

@RestController
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    @GetMapping("/send")
    public String getSmsPage() {
        return "sendSms";
    }

    @PostMapping("/sms/send")
    public ResponseEntity<?> sendSms(@RequestBody MessageDTO messageDto) throws JsonProcessingException, RestClientException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
        SmsResponseDTO response = smsService.sendSms(messageDto);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/sms")
    public ResponseEntity<?> requestSms(
            @RequestParam("requestId") String requestId
//            @RequestParam("time") String time,
//            @RequestParam("key") String key
    ) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, URISyntaxException {
        return ResponseEntity.ok().body(smsService.resultSms(requestId));
    }

}
