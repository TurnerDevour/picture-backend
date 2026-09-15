package com.example.picturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.Space;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@ApiModel(description = "空间级别视图对象")
public class SpaceLevelVO implements Serializable {

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @ApiModelProperty(value = "空间级别：0-普通版 1-专业版 2-旗舰版")
    private Integer value;

    /**
     * 空间名称
     */
    @ApiModelProperty(value = "空间名称")
    private String text;

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

    private static final long serialVersionUID = 1L;
}
