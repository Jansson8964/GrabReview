package com.song.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.song.dto.Result;
import com.song.entity.Follow;
import com.song.mapper.FollowMapper;
import com.song.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.song.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long followUserId, Boolean isFollowed) {
        // 0.get the current user id
        Long userId = UserHolder.getUser().getId();
        // 1.check if the user is followed or not
        if (isFollowed) {
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
        } else {
            LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
            remove(wrapper.eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followUserId));
            // 5.return the result
        }
        return Result.ok("follow successfully");
    }

    @Override
    public Result isFollowed(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        // 2. select count(*) from tb_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();

        return Result.ok(count > 0);
    }
}
