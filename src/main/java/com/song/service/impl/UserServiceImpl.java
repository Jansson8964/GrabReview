package com.song.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.song.dto.LoginFormDTO;
import com.song.dto.Result;
import com.song.dto.UserDTO;
import com.song.entity.User;
import com.song.mapper.UserMapper;
import com.song.service.IUserService;
import com.song.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.song.utils.RedisConstants.*;
import static com.song.utils.RedisConstants.LOGIN_USER_TTL;
import static com.song.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // TODO 发送短信验证码并保存验证码
        // 1. validate phone
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. if phone is invalid, return error message
            return Result.fail("phone is invalid");
        }
        // 3.if phone is valid, send 6-digit code to phone
        String code = RandomUtil.randomNumbers(6);
        // 4. save code to redis, key is telephone number
        // StringRedisTemplate is used to save string
        // phone is key, code is value, expire time is 2 minutes
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 5. send code to phone
        log.debug("success to send code,code: {}", code);
        // 6. return success message
        return Result.ok("success to send code");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // implement login logic
        // 1. validate phone
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. if phone is invalid, return error message
            return Result.fail("phone is invalid");
        }
        // 3. validate code
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        log.info("cacheCode: {}", cacheCode);
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            // 4. if code is invalid, return error message
            return Result.fail("code is invalid");
        }
        // 5. if code is valid, query user by phone used mybatis-plus
        User user = userService.query().eq("phone", phone).one();
        // 6.check if the user exist
        if (user == null) {
            // 7. if user is not exist, create user
            user = createUserWithPhone(phone);
        }
        // use UUID to create token
        String token = UUID.randomUUID().toString();
        // save  user object as hash map to redis
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        userMap.put("icon", userDTO.getIcon());
        userMap.put("id", String.valueOf(userDTO.getId()));
        userMap.put("nickName", userDTO.getNickName());
        // String key = "login:user:" + token;
        String key = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(key, userMap);
        // set expire time
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.SECONDS);
        // if the user is inactive for 30 minutes, the token will expire, indicating that the user has not interacted with the interceptor.
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        // 1. create user
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        //user.setId(Long.valueOf(RandomUtil.randomNumbers(8)));
        //stringRedisTemplate.opsForValue().set(USER_NICK_NAME_PREFIX + user.getId(), user.getNickName(), LOGIN_USER_TTL, TimeUnit.SECONDS);
        // 2. save user
        userService.save(user);
        return user;
    }
}
