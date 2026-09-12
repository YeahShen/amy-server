#EXTM3U
<#list variants as v>
#EXT-X-STREAM-INF:BANDWIDTH=${v.bandwidth?c},RESOLUTION=${v.width?c}x${v.height?c},CODECS="${codec}"
${v.label}/index.m3u8
</#list>
