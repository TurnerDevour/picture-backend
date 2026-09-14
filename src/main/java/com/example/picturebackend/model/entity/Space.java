package com.example.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import lombok.Data;

/**
 * 空间
 */
@ApiModel(description = "空间")
@Data
@TableName(value = "`space`")
public class Space implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "空间 id")
    private Long id;

    /**
     * 空间名称
     */
    @TableField(value = "space_name")
    @ApiModelProperty(value = "空间名称")
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @TableField(value = "space_level")
    @ApiModelProperty(value = "空间级别：0-普通版 1-专业版 2-旗舰版")
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    @TableField(value = "max_size")
    @ApiModelProperty(value = "空间图片的最大总大小")
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    @TableField(value = "max_count")
    @ApiModelProperty(value = "空间图片的最大数量")
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    @TableField(value = "total_size")
    @ApiModelProperty(value = "当前空间下图片的总大小")
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    @TableField(value = "total_count")
    @ApiModelProperty(value = "当前空间下的图片数量")
    private Long totalCount;

    /**
     * 创建用户id
     */
    @TableField(value = "user_id")
    @ApiModelProperty(value = "创建用户id")
    private Long userId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 编辑时间
     */
    @TableField(value = "edit_time")
    @ApiModelProperty(value = "编辑时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "is_delete")
    @TableLogic
    @ApiModelProperty(value = "是否删除")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
