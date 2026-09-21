package com.example.picturebackend.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.exceptions.TooManyResultsException;

import java.util.List;

/**
 * 自定义 BaseMapper，重写 selectOne 使用 selectList 查询，
 * 避免 MyBatis-Plus 3.5.17 默认基于游标（Cursor）的 selectOne 与 ShardingSphere 5.x
 * 不兼容（ShardingSphere 不支持 Statement#closeOnCompletion）导致的问题。
 */
public interface MyBaseMapper<T> extends BaseMapper<T> {

    @Override
    default T selectOne(Wrapper<T> queryWrapper) {
        return selectOne(queryWrapper, true);
    }

    @Override
    default T selectOne(Wrapper<T> queryWrapper, boolean throwEx) {
        List<T> list = selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() > 1 && throwEx) {
            throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
        }
        return list.get(0);
    }
}
