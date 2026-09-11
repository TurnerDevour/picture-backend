package com.example.picturebackend.model.dto.picture;

import com.example.picturebackend.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 图片查询传输对象
 */
@ApiModel(description = "图片查询传输对象")
@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryDTO extends PageRequest implements Serializable {
    /**
     * id
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 图片名称
     */
    @ApiModelProperty(value = "图片名称")
    private String name;

    /**
     * 简介
     */
    @ApiModelProperty(value = "简介")
    private String introduction;

    /**
     * 分类
     */
    @ApiModelProperty(value = "分类")
    private String category;

    /**
     * 标签（JSON 数组）
     */
    @ApiModelProperty(value = "标签（JSON 数组）")
    private List<String> tags;

    /**
     * 图片体积
     */
    @ApiModelProperty(value = "图片体积")
    private Long picSize;

    /**
     * 图片宽度
     */
    @ApiModelProperty(value = "图片宽度")
    private Integer picWidth;

    /**
     * 图片高度
     */
    @ApiModelProperty(value = "图片高度")
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    @ApiModelProperty(value = "图片宽高比例")
    private Double picScale;

    /**
     * 图片格式
     */
    @ApiModelProperty(value = "图片格式")
    private String picFormat;

    /**
     * 创建用户 id
     */
    @ApiModelProperty(value = "创建用户 id")
    private Long userId;

    /**
     * 搜索关键字
     */
    @ApiModelProperty(value = "搜索关键字")
    private String searchText;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    @ApiModelProperty(value = "状态：0-待审核; 1-通过; 2-拒绝")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @ApiModelProperty(value = "审核信息")
    private String reviewMessage;

    /**
     * 审核人 id
     */
    @ApiModelProperty(value = "审核人 id")
    private Long reviewerId;



    private static final long serialVersionUID = 1L;
}
