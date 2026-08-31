package site.ashenstation.amyserver.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SseMessageEvent {
    //    update:status
    UPLOAD_STATUS("update:status");

    private final String type;
}
