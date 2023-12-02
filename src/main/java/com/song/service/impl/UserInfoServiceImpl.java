package com.song.service.impl;

import com.song.entity.UserInfo;
import com.song.mapper.UserInfoMapper;
import com.song.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
