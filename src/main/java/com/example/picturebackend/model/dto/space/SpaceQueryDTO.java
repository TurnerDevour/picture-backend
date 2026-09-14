package com.example.picturebackend.model.dto.space;

import com.example.picturebackend.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@ApiModel(description = "查询空间DTO")
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryDTO extends PageRequest implements Serializable {

    /**
     * 空间 id
     */
    @ApiModelProperty(value = "空间 id")
    private Long id;

    /**
     * 创建用户id
     */
    @ApiModelProperty(value = "创建用户id")
    private Long userId;

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

    private static final long serialVersionUID = 1L;
}

