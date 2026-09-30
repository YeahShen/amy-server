package site.ashenstation.modules.media.service;

import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.ashenstation.utils.FFmpegUtils;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class VideoConversionService {

    private final FFmpegUtils fFmpegUtils;

    /**
     * 异步执行单个档位的转码，返回的 future 在转换完成后结束；失败时以异常结束
     *
     * @param fFmpegExecutor 工作目录已设为任务目录的 FFmpeg 执行器
     * @param source         源文件路径
     * @param target         目标文件（相对执行器工作目录）
     * @param cmd            模板渲染出的 ffmpeg 参数
     * @return 目标路径
     */
    @Async("AmyTaskExecutor")
    public CompletableFuture<String> convertVideo(FFmpegExecutor fFmpegExecutor, String source, String target, String cmd, ProgressListener progressListener) {
        fFmpegUtils.conversion(source, target, cmd, fFmpegExecutor, progressListener);
        return CompletableFuture.completedFuture(target);
    }
}
