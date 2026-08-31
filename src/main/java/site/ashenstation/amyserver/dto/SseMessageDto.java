package site.ashenstation.amyserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import site.ashenstation.amyserver.enums.SseMessageEvent;

import java.util.Map;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SseMessageDto {
    private SseMessageEvent event;
    private Map<String, Object> data;
}
