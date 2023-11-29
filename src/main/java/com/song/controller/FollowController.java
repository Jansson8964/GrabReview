package com.song.controller;


import com.song.dto.Result;
import com.song.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId, @PathVariable("isFollow") Boolean isFollowed) {
        return followService.follow(followUserId, isFollowed);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollowed(@PathVariable("id") Long followUserId) {
        return followService.isFollowed(followUserId);

    }
}
