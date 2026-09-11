package com.example.picturebackend.model.dto.picture;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "图片审核传输对象")
public class PictureReviewDTO implements Serializable {

    /**
     * 审核图片id
     */
    @ApiModelProperty(value = "审核图片id")
    private Long id;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    @ApiModelProperty(value = "审核状态")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @ApiModelProperty(value = "审核信息")
    private String reviewMessage;


    private static final long serialVersionUID = 1L;
}
