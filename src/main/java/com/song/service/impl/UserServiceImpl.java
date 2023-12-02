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
import com.song.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.song.utils.RedisConstants.*;
import static com.song.utils.RedisConstants.LOGIN_USER_TTL;
import static com.song.utils.SystemConstants.USER_NICK_NAME_PREFIX;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

    @Override
    public Result sendCode(String phone, HttpSession session) {
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

    @Override
    public Result queryUserById(Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    @Override
    public Result sign() {
        // get the login user
        Long userId = UserHolder.getUser().getId();
        // get the local datetime
        LocalDateTime now = LocalDateTime.now();
        // splice the key
        String KeySuffix = now.format(DateTimeFormatter.ofPattern(":yyyy-MM"));
        String key = USER_SIGN_KEY + userId + KeySuffix;
        int dayOfMonth = now.getDayOfMonth();
        // save the sign data to redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }


    public Result signCount() {
        //1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        //3. 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        //4. 获取今天是当月第几天(1~31)
        int dayOfMonth = now.getDayOfMonth();
        //5. 获取截止至今日的签到记录  BITFIELD key GET uDay 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }
        //6. 循环遍历
        int count = 0;
        Long num = result.get(0);
        while (true) {
            if ((num & 1) == 0) {
                break;
            } else
                count++;
            //数字右移，抛弃最后一位
            num >>>= 1;
        }
        return Result.ok(count);
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
