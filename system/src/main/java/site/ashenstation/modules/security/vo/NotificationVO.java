package site.ashenstation.modules.security.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
@AllArgsConstructor
public class NotificationVO {
    private String type;
    private Map<String, Object> data;
}
