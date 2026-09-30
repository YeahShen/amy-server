package site.ashenstation.model.entity;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class VideoCollection {

    /**
     * 合集ID (主键, varchar(255))
     */
    private String id;

    /**
     * 创建者用户ID (bigint)
     */
    private Long userId;

    /**
     * 艺术家ID (bigint)
     */
    private Long artistId;

    /**
     * 合集标题 (varchar(150))
     */
    private String title;

    /**
     * 合集简介 (varchar(1000))
     */
    private String description;

    /**
     * 封面图URL (varchar(500))
     */
    private String coverUrl;

    /**
     * 封面选取的视频ID (varchar(99))
     */
    private String coverVideoId;

    /**
     * 视频数量(冗余) (int)
     */
    private Integer videoCount;

    /**
     * 总播放量(冗余) (bigint)
     */
    private Long playCount;

    /**
     * 排序值,越小越靠前 (int)
     */
    private Integer sortOrder;

    /**
     * 状态:0-已删除,1-正常,2-审核中,3-已下架 (tinyint)
     */
    private Integer status;

    /**
     * 创建时间 (datetime)
     */
    private Date createdAt;

    /**
     * 更新时间 (datetime)
     */
    private Date updatedAt;
}
