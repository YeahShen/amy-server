package site.ashenstation.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

@Slf4j
public class FileUtils extends cn.hutool.core.io.FileUtil {

    public static void mergeFileChunk(File destFile, List<File> chunks) throws IOException {
        long totalBytes = 0;

        // 按字节计算进度：汇总所有分片大小作为分母
        for (File chunk : chunks) {
            totalBytes += chunk.length();
        }

        if (totalBytes == 0) {
            throw new IOException("分片文件大小均为 0，无法合并: " + destFile.getAbsolutePath());
        }

        long mergedBytes = 0;
        int lastRate = -1; // 仅在百分比变化时上报，最多 101 条
        byte[] buffer = new byte[8192];

        try (OutputStream out = Files.newOutputStream(destFile.toPath())) {
            for (File chunk : chunks) {
                try (InputStream in = Files.newInputStream(chunk.toPath())) {
                    int n;
                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                        mergedBytes += n;
                        int rate = (int) (mergedBytes * 100 / totalBytes);
                        if (rate != lastRate) {
                            lastRate = rate;
                        }
                    }
                }
            }
        }

        log.info("合并 {} 个分片: {}", chunks.size(), destFile.getName());
    }
}
