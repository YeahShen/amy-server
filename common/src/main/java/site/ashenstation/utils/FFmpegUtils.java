package site.ashenstation.utils;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.RunProcessFunction;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FFmpegUtils {


    @Value("${ffmpeg.ffmpeg-executor-path}")
    private String ffmpegExecutorPath;

    @Value("${ffmpeg.ffprobe-executor-path}")
    private String ffprobeExecutorPath;

    public FFmpeg getFFmpeg() throws IOException {
        return new FFmpeg(ffmpegExecutorPath);
    }

    public FFmpeg getFFmpeg(RunProcessFunction runProcessFunction) throws IOException {
        return new FFmpeg(ffmpegExecutorPath, runProcessFunction);
    }

    public FFprobe getFFprobe() throws IOException {
        return new FFprobe(ffprobeExecutorPath);
    }

    public FFprobe getFFprobe(RunProcessFunction runProcessFunction) throws IOException {
        return new FFprobe(ffprobeExecutorPath, runProcessFunction);
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
                return new VideoResolution(stream.width, stream.height);
            }
        }
        return new VideoResolution(1920, 1080);
    }

    /**
     * 单个清晰度档位：目录名、分辨率、峰值码率（bps，用于 master 播放列表的 BANDWIDTH）
     */
    public record VideoVariant(String label, int width, int height, int bandwidth) {
    }

    /**
     * 按源分辨率筛选可产出的档位（3840×2160 → 848×480）
     * <p>
     * 最低保底 1080p/720p/480p 三档：源分辨率不足（或探测不到）时照常产出，
     * 升采样与黑边补足由模板里的 scale+pad 完成；4k/2k 仍要求源分辨率不小于档位，避免大幅升采样。
     *
     * @return 从高到低的档位列表（master 播放列表按带宽降序），至少三档
     */
    public List<VideoVariant> getVideoResolutions(int maxWidth, int maxHeight) {
        VideoVariant[] all = {
                new VideoVariant("v4k", 3840, 2160, 20000 * 1000),
                new VideoVariant("v2k", 2560, 1440, 10000 * 1000),
                new VideoVariant("v1080p", 1920, 1080, 5000 * 1000),
                new VideoVariant("v720p", 1280, 720, 2500 * 1000),
                new VideoVariant("v480p", 848, 480, 1200 * 1000)
        };

        // 保底按 1080p 对待：源更小时 1080p/720p/480p 三档仍产出
        int effectiveWidth = Math.max(maxWidth, 1920);
        int effectiveHeight = Math.max(maxHeight, 1080);

        List<VideoVariant> result = new ArrayList<>();
        for (VideoVariant variant : all) {
            if (variant.width() <= effectiveWidth && variant.height() <= effectiveHeight) {
                result.add(variant);
            }
        }
        // 声明为高到低，过滤后仍是高到低，与 master 播放列表的带宽降序一致
        return result;
    }

    /**
     * 获取视频编码格式（取第一个视频流的 codec_name，如 h264 / hevc / mpeg4）
     *
     * @return 无视频流或编码未知时返回 null
     */
    public String getEncodingFormat(String videoPath) throws IOException {
        FFmpegProbeResult probe = getFFprobe().probe(videoPath);
        for (FFmpegStream stream : probe.getStreams()) {
            if (stream.codec_type == FFmpegStream.CodecType.VIDEO) {
                return stream.codec_name;
            }
        }
        return null;
    }

    /**
     * 探测转码产物，拼出 HLS master 播放列表所需的 RFC 6381 编码串（CODECS 属性）
     *
     * @return 形如 "avc1.640028,mp4a.40.2"；编码不在映射表内则返回 null，由调用方决定兜底值
     */
    public String getCodecString(String mediaPath) throws IOException {
        FFmpegProbeResult probe = getFFprobe().probe(mediaPath);
        String video = null;
        String audio = null;
        for (FFmpegStream stream : probe.getStreams()) {
            if (stream.codec_type == FFmpegStream.CodecType.VIDEO && video == null) {
                video = switch (stream.codec_name) {
                    case "h264" -> "avc1.640028";
                    case "hevc" -> "hev1.1.6.L120.90";
                    default -> null;
                };
            } else if (stream.codec_type == FFmpegStream.CodecType.AUDIO && audio == null) {
                audio = switch (stream.codec_name) {
                    case "aac" -> "mp4a.40.2";
                    default -> null;
                };
            }
        }

        if (video == null) {
            return null;
        }
        return audio == null ? video : video + "," + audio;
    }


    public void conversion(String source, String target, String args, FFmpegExecutor fFmpegExecutor, ProgressListener progressListener) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source)
                .addOutput(target)
                .addExtraArgs(splitArgs(args))
                .done();

        fFmpegExecutor.createJob(builder, progressListener).run();
    }

    public void conversion(String source, String target, String args, FFmpegExecutor fFmpegExecutor) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source)
                .addOutput(target)
                .addExtraArgs(splitArgs(args))
                .done();

        fFmpegExecutor.createJob(builder).run();
    }

    /**
     * 把模板渲染出的参数串切成 argv。
     * <p>
     * 模板里的参数是照着命令行习惯写的（如 {@code -vf "scale=..."}、{@code -hls_segment_filename "v480p/segment_%03d.ts"}），
     * 但 net.bramp 直接经 ProcessBuilder 传参、不经过 shell，引号会原样进入参数值并被 ffmpeg 当成内容
     * （滤镜图解析器报 "Error parsing filterchain"），故此处剥掉成对的双/单引号、引号内的空白不切分。
     */
    private static String[] splitArgs(String args) {
        List<String> argv = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean started = false;

        for (char c : args.trim().toCharArray()) {
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                started = true;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                started = true;
            } else if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                if (started) {
                    argv.add(token.toString());
                    token.setLength(0);
                    started = false;
                }
            } else {
                token.append(c);
                started = true;
            }
        }
        if (started) {
            argv.add(token.toString());
        }
        return argv.toArray(new String[0]);
    }


}
