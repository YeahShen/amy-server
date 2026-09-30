package site.ashenstation.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Table("mda_album")
@Data
@ToString
public class Album implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 相册ID (主键, varchar(99)，非自增，由应用生成)
     */
    @Id
    private String id;

    /**
     * 所属用户ID (bigint)
     */
    private Long userId;

    /**
     * 艺术家ID (bigint)
     */
    private Long artistId;

    /**
     * 相册名称 (varchar(100))
     */
    private String name;

    /**
     * 相册描述 (varchar(500))
     */
    private String description;

    /**
     * 封面照片 (varchar(99))
     */
    private String coverPhoto;

    /**
     * 照片数量(冗余字段) (int)
     */
    private Integer photoCount;

    /**
     * 排序值,越小越靠前 (int)
     */
    private Integer sortOrder;

    /**
     * 状态:0-已删除,1-正常 (tinyint)
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
