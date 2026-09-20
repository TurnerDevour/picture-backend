package com.example.picturebackend.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间排名分析视图对象
 */
@Data
@ApiModel(description = "空间排名分析视图对象")
public class SpaceRankAnalyzeVO implements Serializable {

    /**
     * 空间 id
     */
    @ApiModelProperty(value = "空间 id")
    private Long id;

    /**
     * 空间名称
     */
    @ApiModelProperty(value = "空间名称")
    private String spaceName;

    /**
     * 创建用户 id
     */
    @ApiModelProperty(value = "创建用户 id")
    private Long userId;

    /**
     * 当前空间下图片的总大小
     */
    @ApiModelProperty(value = "当前空间下图片的总大小")
    private Long totalSize;

    private static final long serialVersionUID = 1L;
}
