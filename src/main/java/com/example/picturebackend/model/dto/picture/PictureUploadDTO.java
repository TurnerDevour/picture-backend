package com.example.picturebackend.model.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 图片
 */
@Data
@ApiModel(description = "图片上传结果")
public class PictureUploadDTO implements Serializable {
    /**
     * id
     */
    @ApiModelProperty(value = "图片id", required = false)
    private Long id;

    /**
     * 图片url
     */
    @ApiModelProperty(value = "图片url", required = false)
    private String url;

    /**
     * 图片名称
     */
    @ApiModelProperty(value = "图片名称", required = false)
    private String picName;


    private static final long serialVersionUID = 1L;
}
