package site.ashenstation.modules.hugefileupload.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LostChunkVo {
    private String taskId;
    private List<Integer> lostChunk;
}
