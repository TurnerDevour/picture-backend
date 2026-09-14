package com.example.picturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Space;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "空间视图对象")
public class SpaceVO implements Serializable {

    /**
     * id
     */
    @ApiModelProperty(value = "空间 id")
    private Long id;

    /**
     * 空间名称
     */
    @ApiModelProperty(value = "空间名称")
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @ApiModelProperty(value = "空间级别：0-普通版 1-专业版 2-旗舰版")
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    @ApiModelProperty(value = "空间图片的最大总大小")
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    @ApiModelProperty(value = "空间图片的最大数量")
    private Long maxCount;

    /**
     * 当前空间下图片的总大小
     */
    @ApiModelProperty(value = "当前空间下图片的总大小")
    private Long totalSize;

    /**
     * 当前空间下的图片数量
     */
    @ApiModelProperty(value = "当前空间下的图片数量")
    private Long totalCount;

    /**
     * 创建用户id
     */
    @ApiModelProperty(value = "创建用户id")
    private Long userId;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 编辑时间
     */
    @ApiModelProperty(value = "编辑时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 创建用户
     */
    @ApiModelProperty(value = "创建用户")
    private UserVO user;

    /**
     * 封装一个类转对象的方法
     *
     * @param spaceVO 空间视图对象
     *
     * @return 返回一个空间视图对象
     */
    public static Space convertVOToObject(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtil.copyProperties(spaceVO, space);
        return space;
    }

    /**
     * 封装一个对象转类的方法
     *
     * @param space 空间对象
     *
     * @return 返回一个空间对象
     */
    public static SpaceVO convertObjectToVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtil.copyProperties(space, spaceVO);
        return spaceVO;
    }

    private static final long serialVersionUID = 1L;
}
