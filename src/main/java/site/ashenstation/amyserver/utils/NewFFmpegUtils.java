package site.ashenstation.amyserver.utils;

import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.RunProcessFunction;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.stereotype.Component;
import site.ashenstation.amyserver.property.FFmpegProperties;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class NewFFmpegUtils {
    private final FFmpegProperties fFmpegProperties;

    public FFmpeg getFFmpeg() throws IOException {
        return new FFmpeg(fFmpegProperties.getFfprobeExecutorPath());
    }

    public FFmpeg getFFmpeg(RunProcessFunction runProcessFunction) throws IOException {
        return new FFmpeg(fFmpegProperties.getFfprobeExecutorPath(), runProcessFunction);
    }

    public FFprobe getFFprobe() throws IOException {
        return new FFprobe(fFmpegProperties.getFfprobeExecutorPath());
    }

    public FFprobe getFFprobe(RunProcessFunction runProcessFunction) throws IOException {
        return new FFprobe(fFmpegProperties.getFfprobeExecutorPath(), runProcessFunction);
    }

    public FFmpegExecutor getExecutor(String workDirectory) throws IOException {
        return this.getExecutor(new File(workDirectory));
    }

    public FFmpegExecutor getExecutor(File workDirectory) throws IOException {
        RunProcessFunction runProcessFunction = new RunProcessFunction();

        runProcessFunction.setWorkingDirectory(workDirectory);

        FFmpeg fFmpeg = this.getFFmpeg(runProcessFunction);
        FFprobe fFprobe = this.getFFprobe(runProcessFunction);

        return new FFmpegExecutor(fFmpeg, fFprobe);
    }

    public Long getDuration(String videoPath) throws IOException {
        FFprobe fFprobe = this.getFFprobe();
        FFmpegProbeResult probe = fFprobe.probe(videoPath);
        
        return Math.round(probe.getFormat().duration);
    }
}
