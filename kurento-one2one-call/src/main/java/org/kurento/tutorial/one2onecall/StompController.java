package org.kurento.tutorial.one2onecall;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.IceCandidate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class StompController {
    @MessageMapping("/webrtc-events")
    public void one2one(@Payload JsonNode stompPayload, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        try {
            processReq(stompPayload, sessionId);
        } catch (Exception e) {
            log.error("Error!", e);
        }
    }

    public void processReq(JsonNode jsonMessage, String session) throws Exception {
        UserSession user = registry.getBySession(session);

        if (user != null) {
            log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
        } else {
            log.debug("Incoming message from new user: {}", jsonMessage);
        }

        switch (jsonMessage.get("id").asText()) {
            case "register":
                try {
                    register(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "registerResponse");
                }
                break;
            case "call":
                try {
                    call(user, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "callResponse");
                }
                break;
            case "incomingCallResponse":
                incomingCallResponse(user, jsonMessage);
                break;
            case "onIceCandidate": {
                JsonNode candidate = jsonMessage.get("candidate");

                if (user != null) {
                    log.info("Adding ice candidate {} to {}", candidate.get("candidate").asText(), user.getName());
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").asText(), candidate.get("sdpMid")
                            .asText(), candidate.get("sdpMLineIndex").asInt());
                    user.addCandidate(cand);
                }
                break;
            }
            case "stop":
                stop(session);
                break;
            default:
                break;
        }
    }
}
