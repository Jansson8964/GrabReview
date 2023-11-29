package com.song.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.song.dto.Result;
import com.song.dto.UserDTO;
import com.song.entity.Blog;
import com.song.entity.User;
import com.song.mapper.BlogMapper;
import com.song.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.song.service.IUserService;
import com.song.utils.SystemConstants;
import com.song.utils.UserHolder;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.song.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        blogService.save(blog);
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result likeBlog(Long id) {
        // get login user
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;
        // check if the login user has liked this blog
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // if hasn't liked, add like count
            // 当在一个类的非静态方法内部使用 this 时，它指的是当前类的一个实例。
            // 这里省略this也可
            boolean isSuccess = update().eq("id", id).setSql("liked = liked + 1").update();
            if (isSuccess) {
                // add user id to redis set
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // if has liked, remove like
            boolean isSuccess = update().eq("id", id).setSql("liked = liked - 1").update();
            if (isSuccess) {
                // add user id to redis set
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @Override
    public Result queryHotBlog(Integer current) {

        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    private void isBlogLiked(Blog blog) {
        // 1. get login user
        Long userId = UserHolder.getUser().getId();
        if (userId == null) {
            return;
        }
        // 2. check if the login user has liked this blog
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    private void queryBlogUser(Blog blog) {
        // 1. get user id
        Long userId = blog.getUserId();
        // 2. query user by id
        User user = userService.getById(userId);
        // 3. set user info to blog
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
    }

    @Override
    public Result queryBlogById(Long id) {
        // query user by id
        Blog blog = blogService.getById(id);
        if (blog == null) {
            return Result.fail("blog not exist");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 5 us`er ids
        Set<String> userIds = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 4);
        if (userIds == null || userIds.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // use stream to convert string to long
        List<Long> ids = userIds.stream().map(Long::valueOf).collect(Collectors.toList());
        String idsStr = StrUtil.join(",", ids);
        // use stream to get userdto
        List<UserDTO> userDTOS = userService.query().in("id", ids).last("order by field(id," + idsStr + ")").
                list().stream().
                map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
