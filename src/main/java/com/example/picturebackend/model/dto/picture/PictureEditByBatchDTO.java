package com.example.picturebackend.model.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel(description = "批量编辑图片信息 DTO")
public class PictureEditByBatchDTO implements Serializable {

    /**
     * 图片 id 列表
     */
    @ApiModelProperty(value = "图片 id 列表")
    private List<Long> pictureIdList;

    /**
     * 空间 id
     */
    @ApiModelProperty(value = "空间 id")
    private Long spaceId;

    /**
     * 命名规则
     */
    @ApiModelProperty(value = "命名规则")
    private String nameRule;

    /**
     * 分类
     */
    @ApiModelProperty(value = "分类")
    private String category;

    /**
     * 标签
     */
    @ApiModelProperty(value = "标签")
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
