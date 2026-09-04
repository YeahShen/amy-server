package site.ashenstation.amyserver.utils;

import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import site.ashenstation.amyserver.property.FFmpegProperties;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FFMpegUtils implements InitializingBean {
    private final FFmpegProperties fFmpegProperties;
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private FFmpegExecutor executor;

    public static final String CONVERSION_TO_MP4_ARGS = "-y -c:v libx264 -preset medium -crf 10 -c:a aac -b:a 320k";
    public static final String CONVERSION_TO_TS_ARGS = "-y -vcodec copy -acodec copy";
    public static final String CONVERSION_TO_M3U8_4K_ARGS = "-y -vf scale=3840:2160:force_original_aspect_ratio=decrease,pad=3840:2160:(ow-iw)/2:(oh-ih)/2 -c:v libx264 -b:v 15000k -g 48 -sc_threshold 0 -c:a aac -b:a 128k -hls_time 6 -hls_playlist_type vod -hls_segment_filename segment_%d.ts";
    public static final String CONVERSION_TO_M3U8_2K_ARGS = "-y -vf scale=2560:1440:force_original_aspect_ratio=decrease,pad=2560:1440:(ow-iw)/2:(oh-ih)/2 -c:v libx264 -b:v 8000k -g 48 -sc_threshold 0 -c:a aac -b:a 128k -hls_time 6 -hls_playlist_type vod -hls_segment_filename segment_%d.ts";
    public static final String CONVERSION_TO_M3U8_1080P_ARGS = "-y -vf scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2 -c:v libx264 -b:v 5000k -g 48 -sc_threshold 0 -c:a aac -b:a 128k -hls_time 6 -hls_playlist_type vod -hls_segment_filename segment_%d.ts";
    public static final String CONVERSION_TO_M3U8_720P_ARGS = "-y -vf scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2 -c:v libx264 -b:v 2500k -g 48 -sc_threshold 0 -c:a aac -b:a 128k -hls_time 6 -hls_playlist_type vod -hls_segment_filename segment_%d.ts";
    public static final String CONVERSION_TO_M3U8_480P_ARGS = "-y -vf scale=854:480:force_original_aspect_ratio=decrease,pad=854:480:(ow-iw)/2:(oh-ih)/2 -c:v libx264 -b:v 1200k -g 48 -sc_threshold 0 -c:a aac -b:a 128k -hls_time 6 -hls_playlist_type vod -hls_segment_filename segment_%d.ts";

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

    /**
     * 获取视频分辨率（取第一个视频流）
     *
     * @return 无视频流时返回 null
     */
    public VideoResolution getVideoResolution(String videoPath) throws IOException {

        FFmpegProbeResult probe = ffprobe.probe(videoPath);
        for (FFmpegStream stream : probe.getStreams()) {
            if (stream.codec_type == FFmpegStream.CodecType.VIDEO) {
                return new VideoResolution(stream.width, stream.height);
            }
        }
        return null;
    }

    /** 视频分辨率值对象 */
    public record VideoResolution(int width, int height) {
        @Override
        public String toString() {
            return width + "x" + height;
        }
    }

    /**
     * 按源高度选择 m3u8 转码档位。
     * 规则：取不超过源高度的最高档（scale 的 force_original_aspect_ratio=decrease 只缩小不放大，
     * 选高于源高度的档位只会浪费码率）。
     *
     * @param sourceHeight 源视频高度（横向视频的“p”数，如 1080）
     */
    public static String resolveM3U8ConversionArgs(int sourceHeight) {
        if (sourceHeight >= 2160) {
            return CONVERSION_TO_M3U8_4K_ARGS;
        }
        if (sourceHeight >= 1440) {
            return CONVERSION_TO_M3U8_2K_ARGS;
        }
        if (sourceHeight >= 1080) {
            return CONVERSION_TO_M3U8_1080P_ARGS;
        }
        if (sourceHeight >= 720) {
            return CONVERSION_TO_M3U8_720P_ARGS;
        }
        return CONVERSION_TO_M3U8_480P_ARGS;
    }

    /** 按源分辨率选择 m3u8 转码档位，见 {@link #resolveM3U8ConversionArgs(int)} */
    public static String resolveM3U8ConversionArgs(VideoResolution resolution) {
        return resolveM3U8ConversionArgs(resolution.height());
    }

    public void conversion(File source, File target, String args) {
        this.conversion(source, target, args, new ProgressListener() {
            @Override
            public void progress(Progress progress) {
            }
        });
    }

    public void conversion(File source, File target, String args, ProgressListener progressListener) {

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .addOutput(target.getAbsolutePath())
                .addExtraArgs(args.trim().split("\\s+"))
                .done();

        this.executor.createJob(builder).run();
    }
}
