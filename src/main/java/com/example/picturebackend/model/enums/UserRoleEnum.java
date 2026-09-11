package com.example.picturebackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {

    ADMIN("管理员", "admin"),
    USER("用户", "user");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }


    /**
     * 根据值获取枚举
     *
     * @param value 枚举值
     *
     * @return 枚举对象
     */
    public static UserRoleEnum getEnumValue(String value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }

        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return null;
    }
}
