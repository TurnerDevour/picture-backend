package com.example.picturebackend.model.dto.space;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "编辑空间DTO")
public class SpaceEditDTO implements Serializable {

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

    private static final long serialVersionUID = 1L;
}
