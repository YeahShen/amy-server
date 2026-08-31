package site.ashenstation.amyserver.utils;

import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import site.ashenstation.amyserver.property.FFmpegProperties;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FFMpegUtils implements InitializingBean {
    private final FFmpegProperties fFmpegProperties;
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private FFmpegExecutor executor;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.ffprobe = new FFprobe(fFmpegProperties.getFfprobeExecutorPath());
        this.ffmpeg = new FFmpeg(fFmpegProperties.getFfmpegExecutorPath());

        this.executor = new FFmpegExecutor(ffmpeg, ffprobe);
    }

    public Long getDuration(String videoPath) throws IOException {

        FFmpegProbeResult probe = ffprobe.probe(videoPath);
        return Math.round(probe.getFormat().duration);
    }

    public void conversionToMP4(String source, String target, ProgressListener progressListener) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source)
                .addOutput(target)
                .addExtraArgs("-c:v", "libx264", "-b:v", "2M", "-preset", "medium", "-crf", "18", "-c:a", "aac", "-b:a", "320k")
                .done();

        executor.createJob(builder, progressListener).run();
    }

    public void conversionToTs(String source, String target, ProgressListener progressListener) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source)
                .addOutput(target)
                .addExtraArgs("-y", "-vcodec", "copy", "-acodec", "copy")
                .done();

        executor.createJob(builder, progressListener).run();
    }

    public void conversionToM38u(String source, String target, ProgressListener progressListener) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source)
                .overrideOutputFiles(true)
                .addOutput(target)
                .setFormat("hls")
                .addExtraArgs("-c", "copy")
                .addExtraArgs("-map", "0")
                .addExtraArgs("-hls_time", "10") // 每个TS切片的目标时长（单位：秒），例如10秒
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-hls_segment_filename", target.replace(".m3u8", "_%03d.ts"))
                .done();

        executor.createJob(builder, progressListener).run();
    }
}
