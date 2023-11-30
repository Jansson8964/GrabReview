package com.song.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.song.dto.Result;
import com.song.dto.UserDTO;
import com.song.entity.Follow;
import com.song.mapper.FollowMapper;
import com.song.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.song.service.IUserService;
import com.song.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.song.utils.RedisConstants.FOLLOWS_KEY;


@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;
    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 0.get the current user id
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 1.check if the user is followed or not
        if (isFollow) {
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            // if success, add the followUserId to the redis set
            if (isSuccess) {
                // 2. add the followUserId to the redis set
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            } else {
                // unfollow
                boolean unfollowSuccess = remove(new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followUserId));
                if (unfollowSuccess) {
                    // remove the followUserId from the redis set
                    stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
                }
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollowed(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        // 2. select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result commonFollow(Long followUserId) {
        Long myId = UserHolder.getUser().getId();
        String myKey = FOLLOWS_KEY + myId;
        String otherKey = FOLLOWS_KEY + followUserId;
        // 1. get the intersection of the two sets
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(myKey, otherKey);
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // convert the set to a String list
        List<Long> collect = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // get the userDTO  list
        // convert the list to a userDTO list
        List<UserDTO> userDTOS = userService.listByIds(collect).stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
