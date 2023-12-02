package com.song.service;

import com.song.dto.Result;
import com.song.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;


public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollowed);

    Result isFollowed(Long followUserId);

    Result commonFollow(Long followUserId);
}
