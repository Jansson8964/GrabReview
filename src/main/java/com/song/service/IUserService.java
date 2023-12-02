package com.song.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.song.dto.LoginFormDTO;
import com.song.dto.Result;
import com.song.entity.User;

import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result queryUserById(Long id);

    Result sign();

    Result signCount();
}
