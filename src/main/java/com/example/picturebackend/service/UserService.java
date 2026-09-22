package com.example.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.model.dto.user.UserAddDTO;
import com.example.picturebackend.model.dto.user.UserQueryDTO;
import com.example.picturebackend.model.dto.user.UserUpdateDTO;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.model.vo.LoginUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     *
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      请求对象
     *
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request 请求对象
     *
     * @return 当前登录用户
     */
    LoginUserVO getCurrentLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户实体（包含全部字段，如密码等，仅在服务端内部使用）
     *
     * @param request 请求对象
     *
     * @return 当前登录用户实体
     */
    User getCurrentLoginUserEntity(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request 请求对象
     *
     * @return 注销结果
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取用户信息
     *
     * @param user 用户对象
     *
     * @return 用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户信息列表
     *
     * @param userList 用户对象列表
     *
     * @return 用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件包装器
     *
     * @param userQueryDTO 用户查询 DTO
     *
     * @return 查询条件包装器
     */
    QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryDTO);

    /**
     * 添加用户
     *
     * @param userAddDTO 用户添加 DTO
     *
     * @return 新用户 id
     */
    long addUser(UserAddDTO userAddDTO);

    /**
     * 根据用户 id 获取用户信息
     *
     * @param userId 用户 id （只有管理员可以调用）
     *
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户 id 获取用户信息（脱敏）
     *
     * @param userId 用户 id
     *
     * @return 用户信息（脱敏）
     */
    UserVO getUserVOById(Long userId);

    /**
     * 根据用户 id 删除用户
     *
     * @param userId 用户 id
     *
     * @return 删除结果
     */
    boolean deleteUserById(Long userId);

    /**
     * 根据用户 id 更新用户信息
     *
     * @param userId        用户 id
     * @param userUpdateDTO 用户更新 DTO
     *
     * @return 更新结果
     */
    boolean updateUserById(Long userId, UserUpdateDTO userUpdateDTO);

    /**
     * 分页查询用户信息
     *
     * @param userQueryDTO 用户查询 DTO
     *
     * @return 用户信息列表
     */
    Page<UserVO> getUserVOListByPage(UserQueryDTO userQueryDTO);

    /**
     * 判断用户是否为管理员
     *
     * @param user 用户对象
     *
     * @return 是否为管理员
     */
    boolean isAdmin(LoginUserVO user);
}

