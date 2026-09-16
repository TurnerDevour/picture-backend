package com.example.picturebackend.model.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "以图搜图请求对象")
public class SearchPictureByPictureDTO implements Serializable {

    /**
     *
     * 图片 id
     */
    @ApiModelProperty(value = "图片 id", required = true)
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
