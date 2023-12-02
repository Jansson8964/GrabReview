package com.song.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.song.dto.Result;
import com.song.dto.ScrollResult;
import com.song.dto.UserDTO;
import com.song.entity.Blog;
import com.song.entity.Follow;
import com.song.entity.User;
import com.song.mapper.BlogMapper;
import com.song.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.song.service.IFollowService;
import com.song.service.IUserService;
import com.song.utils.SystemConstants;
import com.song.utils.UserHolder;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.song.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.song.utils.RedisConstants.FEED_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;


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

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        save(blog);
        LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Follow::getFollowUserId, user.getId());
        List<Follow> follows = followService.list(queryWrapper);
        for (Follow follow : follows) {
            Long userId = follow.getUserId();
            String key = FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1. get login user
        Long userId = UserHolder.getUser().getId();
        // 2. get user's follow
        String KEY = FEED_KEY + userId;
        // 3. query user inbox ZREVRANGEBYSCORE key max min limit offset count
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(KEY, 0, max, offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 4.解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0; // 2
        int os = 1; // 2
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) { // 5 4 4 2 2
            // 4.1.获取id
            ids.add(Long.valueOf(tuple.getValue()));
            // 4.2.获取分数(时间戳）
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }
        os = minTime == max ? os : os + offset;
        // 5.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

        for (Blog blog : blogs) {
            // 5.1.查询blog有关的用户
            queryBlogUser(blog);
            // 5.2.查询blog是否被点赞
            isBlogLiked(blog);
        }

        // 6.封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);

        return Result.ok(r);
    }
}
