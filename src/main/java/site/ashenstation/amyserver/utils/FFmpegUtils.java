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
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FFmpegUtils {
    private final FFmpegProperties fFmpegProperties;

    public final static Integer _4K = 2160;
    public final static Integer _2K = 1440;
    public final static Integer _1080P = 1080;
    public final static Integer _720P = 720;

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

    public void conversion(File source, String args, FFmpegExecutor fFmpegExecutor) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getAbsolutePath())
                .addExtraArgs(args.trim().split("\\s+"));
        fFmpegExecutor.createJob(builder).run();
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

    public record ConversionPlan(String command, List<String> label) {

    }

    /**
     * 生成自适应的 FFmpeg HLS 转码命令（单行，无换行）
     *
     * @param maxWidth     最高分辨率宽度
     * @param maxHeight    最高分辨率高度
     * @param videoEncoder 视频编码器名称（如 "libx264", "h264_nvenc", "h264_amf"）
     * @param audioEncoder 音频编码器名称（如 "aac", "libmp3lame", "copy"）
     * @return 完整的 FFmpeg 命令字符串（单行），若无法匹配返回 null
     */
    public FFmpegUtils.ConversionPlan generateAdaptiveFFmpegCommand(int maxWidth,
                                                                    int maxHeight,
                                                                    String videoEncoder,
                                                                    String audioEncoder) {
        // 预定义分辨率阶梯（从高到低）
        int[][] resolutions = {
                {3840, 2160},  // 4K
                {2560, 1440},  // 2K
                {1920, 1080},  // 1080p
                {1280, 720},   // 720p
                {854, 480}     // 480p
        };
        int[] bitrates = {15000, 8000, 5000, 2500, 1200};
        String[] labels = {"_4k", "_2k", "_1080p", "_720p", "_480p"};

        // 筛选有效分辨率
        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i][0] <= maxWidth && resolutions[i][1] <= maxHeight) {
                validIndices.add(i);
            }
        }
        if (validIndices.isEmpty()) {
            return null;
        }

        int n = validIndices.size();
        StringBuilder cmd = new StringBuilder();

        // 基础命令头
        cmd.append(" -y ");

        // ---------- 构建 filter_complex ----------
        cmd.append("-filter_complex \"");

        // 视频分流
        cmd.append("[0:v]split=").append(n);
        for (int i = 0; i < n; i++) {
            cmd.append("[v").append(i + 1).append("]");
        }
        cmd.append(";");

        List<String> _label = new ArrayList<>();

        // 每个分支应用 scale+pad
        for (int i = 0; i < n; i++) {
            int idx = validIndices.get(i);
            int w = resolutions[idx][0];
            int h = resolutions[idx][1];
            cmd.append("[v").append(i + 1).append("]")
                    .append("scale=").append(w).append(":").append(h)
                    .append(":force_original_aspect_ratio=decrease,")
                    .append("pad=").append(w).append(":").append(h)
                    .append(":(ow-iw)/2:(oh-ih)/2")
                    .append("[out").append(i + 1).append("]; ");
        }

        // 音频分流
        cmd.append("[0:a]asplit=").append(n);
        for (int i = 0; i < n; i++) {
            cmd.append("[a").append(i + 1).append("]");
        }
        cmd.append("\" ");


        // ---------- 每个输出的映射和参数 ----------
        for (int i = 0; i < n; i++) {
            int idx = validIndices.get(i);
            String label = labels[idx];
            int bitrate = bitrates[idx];

            _label.add(label.replace("_", ""));

            cmd.append("-map \"[out").append(i + 1).append("]\" ")
                    .append("-map \"[a").append(i + 1).append("]\" ")
                    .append("-c:v ").append(videoEncoder).append(" -b:v ").append(bitrate).append("k ")
                    .append("-g 48 -sc_threshold 0 ")
                    .append("-c:a ").append(audioEncoder);

            // 若音频编码器不是 "copy"，则需要指定码率
            if (!"copy".equalsIgnoreCase(audioEncoder)) {
                cmd.append(" -b:a 128k");
            }

            cmd.append(" -hls_time 6 -hls_playlist_type vod ")
                    .append("-hls_segment_filename ").append(label).append("/segment_%d.ts ")
                    .append(label).append("/index.m3u8");

            if (i < n - 1) {
                cmd.append(" ");
            }
        }

        cmd.append(" -master_pl_name ");

//        return cmd.toString();
        return new FFmpegUtils.ConversionPlan(cmd.toString(), _label);
    }

//    // 示例：使用 NVIDIA NVENC 编码器
//    public static void main(String[] args) {
//        ConversionPlan conversionPlan = generateAdaptiveFFmpegCommand(
//                "xx.mp4",
//                3840, 2160,
//                "h264_amf",    // 视频编码器
//                "aac"            // 音频编码器
//        );
//        assert conversionPlan != null;
//        if (conversionPlan.command != null) {
//            System.out.println("单行命令：\n" + conversionPlan.command);
//        } else {
//            System.out.println("无匹配分辨率。");
//        }
//    }
}
