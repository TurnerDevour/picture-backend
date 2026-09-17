package com.example.picturebackend.model.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "根据图片主色调查询图片DTO")
public class SearchPictureByColorDTO implements Serializable {

    /**
     * 图片主色调
     */
    @ApiModelProperty(value = "图片主色调")
    private String picColor;

    /**
     * 空间 id
     */
    @ApiModelProperty(value = "空间 id")
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
