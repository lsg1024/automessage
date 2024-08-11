package excel.automessage.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

import java.io.IOException;
import java.util.Date;


@Slf4j
public class PersistentRememberMeTokenDeserializer extends JsonDeserializer<PersistentRememberMeToken> {

    @Override
    public PersistentRememberMeToken deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String username = node.get("username").asText();
        String series = node.get("series").asText();
        String tokenValue = node.get("tokenValue").asText();

        // date 값은 long으로 이뤄짐
        JsonNode dateNode = node.get("date");
        long date = dateNode.asLong();

        return new PersistentRememberMeToken(username, series, tokenValue, new Date(date));
    }
}