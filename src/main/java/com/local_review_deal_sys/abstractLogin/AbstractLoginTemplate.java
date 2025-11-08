package com.local_review_deal_sys.abstractLogin;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.local_review_deal_sys.dto.LoginFormDTO;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.*;

public abstract class AbstractLoginTemplate {

    protected final StringRedisTemplate stringRedisTemplate;

    public AbstractLoginTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // ★ 模板方法：登录对外只暴露这个
    public Result login(LoginFormDTO loginForm) {
        // 1. 参数校验
        if (!checkParam(loginForm)) {
            return Result.fail("Invalid input");
        }

        // 2. 认证（变化点：不同登录方式不同）
        User user = authenticate(loginForm);
        if(user == null){
            return Result.fail("Authentication failed");
        }

        // 3. 成功后保存 session/token
        String token = saveUserToRedis(user);

        return Result.ok(token);
    }

    // 通用校验
    protected boolean checkParam(LoginFormDTO loginForm) {
        return loginForm != null && loginForm.getPhone() != null;
    }

    // ★ 抽象方法：把认证逻辑交给子类
    protected abstract User authenticate(LoginFormDTO loginForm);

    // 登录成功后写入 redis（通用逻辑）
    protected String saveUserToRedis(User user) {
        String token = UUID.randomUUID().toString(true);

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, value) -> value == null ? null : value.toString()));

        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);

        return token;
    }
}