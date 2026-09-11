package com.example.picturebackend.aop;

import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import com.example.picturebackend.model.enums.UserRoleEnum;
import com.example.picturebackend.model.vo.LoginUserVO;
import com.example.picturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 环绕通知，拦截带有 @AuthCheck 注解的方法
     *
     * @param joinPoint 连接点
     * @param authCheck 认证检查注解
     *
     * @return 方法执行结果
     *
     * @throws Throwable 异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 获取当前登录用户的角色
        String mustRole = authCheck.mustRole();
        // 2. 获取当前请求的用户信息
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 3. 检查用户是否有权限
        LoginUserVO loginUser = userService.getCurrentLoginUser(request);// 获取当前登录用户，若未登录会抛出异常
        // 4. 检查用户角色是否符合要求
        // 5. 如果不需要角色检查，直接放行
        UserRoleEnum roleEnum = UserRoleEnum.getEnumValue(mustRole);
        if (roleEnum == null) {
            return joinPoint.proceed();
        }

        // 6. 如果需要角色检查，判断当前用户的角色是否符合要求
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 7. 要求角色必须是管理员，且当前用户不是管理员，则抛出异常
        if (UserRoleEnum.ADMIN.equals(roleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return joinPoint.proceed();
    }

}
