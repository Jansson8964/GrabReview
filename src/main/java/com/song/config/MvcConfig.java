package com.song.config;

import com.song.utils.LoginInterceptor;
import com.song.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate StringRedisTemplate;
    //1. add login interceptor

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] excludePatterns = new String[]{
                "/user/code",
                "/user/login",
                "/shop/**",  // permit all access to shop
                "/voucher/**",  // permit all access to voucher
                "/shop-type/**",  // permit all access to shop type
                "/order/submit",
                "/upload/**",
                // knife4j swagger
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"
        };
        // 1. add login interceptor
        registry.addInterceptor(new LoginInterceptor())
                // 3. add exclude path pattern
                .excludePathPatterns(
                        excludePatterns
                ).order(1);
        // 2. add refresh token interceptor
        // intercept all requests(default)
        // order 0, higher priority than login interceptor
        registry.addInterceptor(new RefreshTokenInterceptor(StringRedisTemplate)).order(0);
    }
}
