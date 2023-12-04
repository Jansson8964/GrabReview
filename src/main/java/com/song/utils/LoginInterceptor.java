package com.song.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.song.dto.UserDTO;
import com.song.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.song.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * login interceptor, implement HandlerInterceptor
 */
public class LoginInterceptor implements HandlerInterceptor {
    //

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. Check if interception is needed
        // check if used is exist in threadLocal
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO != null) {
            // user exist, no need to intercept
            return true;
        } else {
            response.setStatus(401);
            return false;
        }
    }

}
