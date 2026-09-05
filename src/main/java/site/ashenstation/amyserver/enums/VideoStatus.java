package site.ashenstation.amyserver.enums;

import com.mybatisflex.annotation.EnumValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum VideoStatus {
    NORMAL("normal"), DELETE("delete"), CONVERSION("conversion");


    private final String type;

    @EnumValue
    public String getType() {
        return type;
    }

    public static VideoStatus find(String type) {
        for (VideoStatus value : VideoStatus.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return VideoStatus.NORMAL;
    }
}
