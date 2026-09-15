package com.example.picturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Picture;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片
 */
@ApiModel(description = "图片")
@Data
public class PictureVO implements Serializable {
    /**
     * id
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 图片 url
     */
    @ApiModelProperty(value = "图片 url")
    private String url;

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
     * 缩略图 url
     */
    @ApiModelProperty(value = "缩略图 url")
    private String thumbnailUrl;


    /**
     * 创建用户 id
     */
    @ApiModelProperty(value = "创建用户 id")
    private Long userId;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 编辑时间
     */
    @ApiModelProperty(value = "编辑时间")
    private Date editTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 用户信息
     */
    @ApiModelProperty(value = "用户信息")
    private UserVO user;

    /**
     * 空间 id
     */
    @ApiModelProperty(value = "空间 id")
    private Long spaceId;


    private static final long serialVersionUID = 1L;

    /**
     * 类转换成对象
     */
    public static Picture convertVOToObject(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureVO, picture); // 将 pictureVO 的属性复制到 picture 对象中
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags())); // 将 tags 转换为 JSON 字符串
        return picture;
    }

    /**
     * 对象转换成类
     */
    public static PictureVO convertObjectToVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO); // 将 picture 的属性复制到 pictureVO 对象中
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class)); // 将 tags 转换为 JSON 数组
        return pictureVO;
    }
}
