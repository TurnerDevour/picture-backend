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

    private static final long serialVersionUID = 1L;
}
