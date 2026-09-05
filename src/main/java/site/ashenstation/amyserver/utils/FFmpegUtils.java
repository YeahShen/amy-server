package site.ashenstation.amyserver.utils;

import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.RunProcessFunction;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.stereotype.Component;
import site.ashenstation.amyserver.property.FFmpegProperties;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FFmpegUtils {
    private final FFmpegProperties fFmpegProperties;

    public final static Integer _4K  = 2160;
    public final static Integer _2K  = 1440;
    public final static Integer _1080P  = 1080;
    public final static Integer _720P  = 720;

    public FFmpeg getFFmpeg() throws IOException {
        return new FFmpeg(fFmpegProperties.getFfmpegExecutorPath());
    }

    public FFmpeg getFFmpeg(RunProcessFunction runProcessFunction) throws IOException {
        return new FFmpeg(fFmpegProperties.getFfmpegExecutorPath(), runProcessFunction);
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

    public record VideoResolution(int width, int height) {
        @Override
        public String toString() {
            return width + "x" + height;
        }
    }

    /**
     * 获取视频分辨率（取第一个视频流）
     *
     * @return 无视频流时返回 null
     */
    public FFmpegUtils.VideoResolution getVideoResolution(String videoPath) throws IOException {

        FFmpegProbeResult probe = getFFprobe().probe(videoPath);
        for (FFmpegStream stream : probe.getStreams()) {
            if (stream.codec_type == FFmpegStream.CodecType.VIDEO) {
                return new FFmpegUtils.VideoResolution(stream.width, stream.height);
            }
        }
        return null;
    }

    public void conversion(File source, File target, String args, FFmpegExecutor fFmpegExecutor) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .addOutput(target.getAbsolutePath())
                .addExtraArgs(args.trim().split("\\s+"))
                .done();

        fFmpegExecutor.createJob(builder).run();
    }

    public void conversion(File source, File target, String args, FFmpegExecutor fFmpegExecutor, ProgressListener progressListener) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .addOutput(target.getAbsolutePath())
                .addExtraArgs(args.trim().split("\\s+"))
                .done();

        fFmpegExecutor.createJob(builder, progressListener).run();
    }

}
