package site.ashenstation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginPlatform {

    CLIENT("client"),
    BROWSER("browser"),
    MOBILE("mobile");

    private final String type;

    public static LoginPlatform find(String type) {
        for (LoginPlatform value : LoginPlatform.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return LoginPlatform.CLIENT;
    }
}
