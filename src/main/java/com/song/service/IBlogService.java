package com.song.service;

import com.song.dto.Result;
import com.song.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result saveBlog(Blog blog);

    Result likeBlog(Long id);

    Result queryMyBlog(Integer current);

    Result queryHotBlog(Integer current);

    Result queryBlogById(Long id);

    Result queryBlogLikes(Long id);

    Result queryBlogOfFollow(Long max, Integer offset);
}
