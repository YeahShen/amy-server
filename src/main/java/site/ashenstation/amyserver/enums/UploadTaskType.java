package site.ashenstation.amyserver.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UploadTaskType {
    VIDEO("video");

    private final String type;
}
