package site.ashenstation.model.entity;

import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ToString
public class AlbumPhoto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 照片ID
     */
    private Long id;

    /**
     * 所属相册ID
     */
    private String albumId;

    /**
     * 上传用户ID(冗余,便于查询)
     */
    private Long userId;

    /**
     * 照片标题
     */
    private String title;

    /**
     * 照片描述
     */
    private String description;

    /**
     * 原图URL
     */
    private String url;

    /**
     * 缩略图URL
     */
    private String thumbUrl;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Integer fileSize;

    /**
     * MIME类型,如image/jpeg
     */
    private String mimeType;

    /**
     * 图片宽度(px)
     */
    private Integer width;

    /**
     * 图片高度(px)
     */
    private Integer height;

    /**
     * EXIF信息(拍摄时间、设备等)
     */
    private String exif;

    /**
     * 相册内排序
     */
    private Integer sortOrder;

    /**
     * 状态:0-已删除,1-正常
     */
    private Integer status;

    /**
     * 上传时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
