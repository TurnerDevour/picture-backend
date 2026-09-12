package com.example.picturebackend.model.dto.picture;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PictureUploadByBatchDTO {

    /**
     * 搜索词
     */
    @ApiModelProperty(value = "搜索词")
    private String searchText;

    /**
     * 抓取数量
     */
    @ApiModelProperty(value = "抓取数量")
    private Integer count = 10;

    /**
     * 名称前缀
     */
    @ApiModelProperty(value = "名称前缀")
    private String namePrefix;

}
