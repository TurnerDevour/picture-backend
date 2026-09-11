package com.example.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.model.dto.user.UserAddDTO;
import com.example.picturebackend.model.dto.user.UserQueryDTO;
import com.example.picturebackend.model.dto.user.UserUpdateDTO;
import com.example.picturebackend.model.dto.user.UserVO;
import com.example.picturebackend.model.enums.UserRoleEnum;
import com.example.picturebackend.model.vo.LoginUserVO;
import org.springframework.stereotype.Service;
import com.example.picturebackend.model.entity.User;
import com.example.picturebackend.mapper.UserMapper;
import com.example.picturebackend.service.UserService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     *
     * @return 新用户 id
     */
    @Override
    @Transactional
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 检查用户账号是否存在且不能重复
        long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("user_account", userAccount)
        );

        if (count > 0) {
            // 用户已存在，返回错误
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败，用户已存在");
        }

        // 2. 密码做加密处理
        String md5Password = encryptPassword(userPassword);

        // 3. 设置随机用户名
        long randomNum = (long) (Math.random() * 10000);
        String randomUsername = "云图用户" + randomNum;

        // 4. 插入数据到数据库中
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(md5Password);
        newUser.setUsername(randomUsername);
        newUser.setUserRole(UserRoleEnum.USER.getValue()); // 设置默认角色为普通用户

        int i = userMapper.insert(newUser);
        if (i == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }

        return newUser.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP 请求
     *
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 密码做加密处理
        String md5Password = encryptPassword(userPassword);

        // 2. 检查用户账号和密码是否匹配
        User loginUser = userMapper.selectOne(
                new QueryWrapper<User>()
                        .eq("user_account", userAccount)
                        .eq("user_password", md5Password)
        );
        if (loginUser == null) {
            log.error("登录失败，用户不存在或密码错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录失败，用户不存在或密码错误");
        }


        // 3. 将用户信息存入 session
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, loginUser);

        return BeanUtil.copyProperties(loginUser, LoginUserVO.class);
    }

    /**
     * 获取当前登录用户
     *
     * @param request HTTP 请求
     *
     * @return 当前登录用户信息
     */
    @Override
    public LoginUserVO getCurrentLoginUser(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            log.error("用户未登录");
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 从数据库中查询最新的用户信息
        loginUser = userMapper.selectById(loginUser.getId());
        if (loginUser == null) {
            log.error("用户不存在");
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        return BeanUtil.copyProperties(loginUser, LoginUserVO.class);
    }

    /**
     * 用户注销
     *
     * @param request HTTP 请求
     *
     * @return 注销结果
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 检查用户是否登录
        Object loginUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            log.error("用户未登录");
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }

        // 移除 session 中的用户信息
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取用户信息
     *
     * @param user 用户对象
     *
     * @return 用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        // 1. 检查用户对象是否为空
        if (ObjectUtil.isEmpty(user)) {
            log.error("用户对象为空");
            return null;
        }
        // 2. 将用户对象转换为用户信息对象
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);

        return userVO;
    }

    /**
     * 获取用户信息列表
     *
     * @param userList 用户对象列表
     *
     * @return 用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            log.error("用户列表为空");
            return List.of();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询条件包装器
     *
     * @param userQueryDTO 用户查询 DTO
     *
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryDTO) {
        if (userQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询条件不能为空");
        }

        Long id = userQueryDTO.getId();
        String username = userQueryDTO.getUsername();
        String userAccount = userQueryDTO.getUserAccount();
        String userProfile = userQueryDTO.getUserProfile();
        String userRole = userQueryDTO.getUserRole();
        String sortField = userQueryDTO.getSortField();
        String sortOrder = userQueryDTO.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.like(StrUtil.isNotBlank(username), "username", username);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);


        return queryWrapper;
    }

    /**
     * 添加用户
     *
     * @param userAddDTO 用户添加 DTO
     *
     * @return 新用户 id
     */
    @Override
    @Transactional
    public long addUser(UserAddDTO userAddDTO) {
        // 1. 密码做加密处理
        final String defaultPassword = "123456"; // 默认密码为 123456
        String md5Password = encryptPassword(defaultPassword);
        // 2. 检查用户账号是否存在且不能重复
        Long count = userMapper.selectCount(new QueryWrapper<User>().eq("user_account", userAddDTO.getUserAccount()));
        if (count > 0) {
            // 用户已存在，返回错误
            log.error("添加失败");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加失败");
        }
        // 3. 设置 user 对象
        User user = new User();
        user.setUserPassword(md5Password);
        BeanUtil.copyProperties(userAddDTO, user);
        // 4. 插入数据到数据库中
        int i = userMapper.insert(user);
        if (i == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加失败");
        }
        return user.getId();
    }

    /**
     * 根据用户 id 获取用户信息
     *
     * @param userId 用户 id
     *
     * @return 用户信息
     */
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 根据用户 id 获取用户信息（脱敏）
     *
     * @param userId 用户 id
     *
     * @return 用户信息（脱敏）
     */
    @Override
    public UserVO getUserVOById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("用户不存在");
            return null;
        }
        return getUserVO(user);
    }

    /**
     * 根据用户 id 删除用户
     *
     * @param userId 用户 id
     *
     * @return 删除结果
     */
    @Override
    @Transactional
    public boolean deleteUserById(Long userId) {
        int i = userMapper.deleteById(userId);
        return i > 0;
    }

    /**
     * 根据用户 id 更新用户信息
     *
     * @param userId        用户 id
     * @param userUpdateDTO 用户更新 DTO
     *
     * @return 更新结果
     */
    @Override
    @Transactional
    public boolean updateUserById(Long userId, UserUpdateDTO userUpdateDTO) {
        // 1. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.error("用户不存在");
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        // 2. 更新用户信息
        User updateUser = new User();
        BeanUtil.copyProperties(userUpdateDTO, updateUser);
        updateUser.setId(userId);
        int i = userMapper.updateById(updateUser);
        return i > 0;
    }

    /**
     * 分页查询用户信息
     *
     * @param userQueryDTO 用户查询 DTO
     *
     * @return 用户信息列表
     */
    @Override
    public Page<UserVO> getUserVOListByPage(UserQueryDTO userQueryDTO) {
        long pageNum = userQueryDTO.getCurrent();
        long pageSize = userQueryDTO.getPageSize();
        QueryWrapper<User> queryWrapper = getQueryWrapper(userQueryDTO);
        // 执行分页查询（此前漏掉这一行，导致 total 和 records 都是空）
        Page<User> userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserVO> userVOList = getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    /**
     * 判断用户是否为管理员
     *
     * @param user 用户对象
     *
     * @return 是否为管理员
     */
    @Override
    public boolean isAdmin(LoginUserVO user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    private String encryptPassword(String password) {
        // 1. 盐值
        String salt = "#$%&@!ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        // 2. 将盐值与密码进行拼接
        String saltedPassword = salt + password;
        // 3. 使用 Hutool 的 DigestUtil 进行 MD5 加密
        return DigestUtil.md5Hex(saltedPassword);
    }
}

