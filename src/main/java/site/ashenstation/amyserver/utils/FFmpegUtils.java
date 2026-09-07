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

    public void conversion(String source, String target, String args, FFmpegExecutor fFmpegExecutor, ProgressListener progressListener) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source)
                .addOutput(target)
                .addExtraArgs(args.trim().split("\\s+"))
                .done();

        fFmpegExecutor.createJob(builder, progressListener).run();
    }

    /**
     * 以参数列表方式转码（参数值含空格时用本方法：逐个元素直传进程。
     * 勿把参数拼成字符串加引号再按空格拆分——ProcessBuilder 不经 shell，引号会被原样传给 ffmpeg）
     */
    public void conversion(String source, String target, List<String> args, FFmpegExecutor fFmpegExecutor, ProgressListener progressListener) {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source)
                .addOutput(target)
                .addExtraArgs(args.toArray(new String[0]))
                .done();

        fFmpegExecutor.createJob(builder, progressListener).run();
    }

    public record ConversionPlan(List<String> command, List<String> label) {

    }

    /**
     * 生成自适应多清晰度 HLS 转码的 FFmpeg 参数列表
     * <p>
     * 注意：返回的是进程参数列表（一个元素 = 一个 argv）。net.bramp 经 ProcessBuilder 直传进程、不经 shell，
     * 因此值内含空格的选项（-filter_complex / -var_stream_map）必须整体作为单个元素，
     * 不能拼进字符串用引号包裹再按空格拆分，否则引号会被当成参数内容导致解析失败。
     *
     * @param maxWidth     最高分辨率宽度
     * @param maxHeight    最高分辨率高度
     * @param videoEncoder 视频编码器名称（如 "libx264", "h264_nvenc", "h264_amf", "h264_qsv"）
     * @param audioEncoder 音频编码器名称（如 "aac", "libmp3lame", "copy"）
     * @return 参数列表（不含输出文件名，输出 target 由调用方传入 "%v/index.m3u8"），若无法匹配返回 null
     */
    public ConversionPlan generateAdaptiveFFmpegCommand(int maxWidth,
                                                        int maxHeight,
                                                        String videoEncoder,
                                                        String audioEncoder) {
        // 1. 预定义分辨率阶梯（从高到低）
        // 注意：480p 宽度使用 848，避免奇数宽导致 H.264 硬件编码器报错
        int[][] resolutions = {
                {3840, 2160},  // 4K
                {2560, 1440},  // 2K
                {1920, 1080},  // 1080p
                {1280, 720},   // 720p
                {848, 480}     // 480p
        };
        int[] bitrates = {15000, 8000, 5000, 2500, 1200};
        // 各档目录/流名（-var_stream_map 的 name 会替换输出里的 %v，目录名须与此一致）
        String[] labels = {"v4k", "v2k", "v1080p", "v720p", "v480p"};

        // 2. 筛选有效分辨率
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
        List<String> args = new ArrayList<>();
        List<String> resultLabels = new ArrayList<>();

        // 覆盖已存在的输出文件
        args.add("-y");

        // ---------- 构建 filter_complex（整段图作为单个参数元素） ----------
        StringBuilder filterComplex = new StringBuilder();

        // 视频分流
        filterComplex.append("[0:v]split=").append(n);
        for (int i = 0; i < n; i++) {
            filterComplex.append("[v").append(i + 1).append("]");
        }
        filterComplex.append(";");

        // 每个视频分支应用 scale + pad
        for (int i = 0; i < n; i++) {
            int idx = validIndices.get(i);
            int w = resolutions[idx][0];
            int h = resolutions[idx][1];
            filterComplex.append("[v").append(i + 1).append("]")
                    .append("scale=").append(w).append(":").append(h)
                    .append(":force_original_aspect_ratio=decrease,")
                    .append("pad=").append(w).append(":").append(h)
                    .append(":(ow-iw)/2:(oh-ih)/2")
                    .append("[out").append(i + 1).append("]; ");
        }

        // 音频分流
        filterComplex.append("[0:a]asplit=").append(n);
        for (int i = 0; i < n; i++) {
            filterComplex.append("[a").append(i + 1).append("]");
        }
        args.add("-filter_complex");
        args.add(filterComplex.toString());

        // ---------- 每个输出的映射和编码参数 ----------
        StringBuilder varStreamMap = new StringBuilder();

        for (int i = 0; i < n; i++) {
            int idx = validIndices.get(i);
            String label = labels[idx];
            int bitrate = bitrates[idx];

            resultLabels.add(label);

            // 映射视频与音频，并绑定流索引编码参数 (:0, :1, :2...)
            args.add("-map");
            args.add("[out" + (i + 1) + "]");
            args.add("-map");
            args.add("[a" + (i + 1) + "]");
            args.add("-c:v:" + i);
            args.add(videoEncoder);
            args.add("-b:v:" + i);
            args.add(bitrate + "k");

            // 动态构建 -var_stream_map 参数（含空格，整体作为单个元素）
            if (i > 0) {
                varStreamMap.append(" ");
            }
            varStreamMap.append("v:").append(i).append(",a:").append(i).append(",name:").append(label);
        }

        // ---------- 公共 HLS 输出选项 ----------
        args.add("-g");
        args.add("48");
        args.add("-sc_threshold");
        args.add("0");
        args.add("-c:a");
        args.add(audioEncoder);
        args.add("-b:a");
        args.add("128k");
        args.add("-f");
        args.add("hls");
        args.add("-hls_time");
        args.add("6");
        args.add("-hls_playlist_type");
        args.add("vod");
        args.add("-hls_segment_filename");
        args.add("%v/segment_%03d.ts");
        args.add("-master_pl_name");
        args.add("index.m3u8");
        args.add("-var_stream_map");
        args.add(varStreamMap.toString());

        return new ConversionPlan(args, resultLabels);
    }
}