package com.example.picturebackend.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

@Data
@ApiModel(description = "分页请求")
public class PageRequest {

    /**
     * 当前页号
     */
    @ApiModelProperty(value = "当前页号", example = "1")
    @Min(value = 1, message = "当前页号必须大于等于1")
    private long current = 1;

    /**
     * 页数大小
     */
    @ApiModelProperty(value = "页数大小", example = "10")
    @Min(value = 10, message = "页数大小必须大于等于10")
    private long pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
