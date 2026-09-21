package com.example.picturebackend.model.vo;

import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.SpaceUser;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "空间用户信息")
public class SpaceUserVO implements Serializable {

    /**
     * id
     */
    @ApiModelProperty(value = "id", example = "1")
    private Long id;

    /**
     * 空间 id
     */
    @ApiModelProperty(value = "空间 id", example = "1")
    private Long spaceId;

    /**
     * 用户 id
     */
    @ApiModelProperty(value = "用户 id", example = "1")
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    @ApiModelProperty(value = "空间角色", example = "viewer")
    private String spaceRole;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间", example = "2023-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间", example = "2023-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 用户信息
     */
    @ApiModelProperty(value = "用户信息")
    private UserVO user;

    /**
     * 空间信息
     */
    @ApiModelProperty(value = "空间信息")
    private SpaceVO space;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     *
     * @param spaceUserVO
     *
     * @return
     */
    public static SpaceUser convertVOToObject(SpaceUserVO spaceUserVO) {
        if (spaceUserVO == null) {
            return null;
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }

    /**
     * 对象转封装类
     *
     * @param spaceUser
     *
     * @return
     */
    public static SpaceUserVO convertObjectToVO(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }
}
