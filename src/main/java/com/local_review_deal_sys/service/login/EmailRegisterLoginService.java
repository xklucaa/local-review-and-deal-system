package com.local_review_deal_sys.service.login;

import com.local_review_deal_sys.abstractLogin.AbstractLoginTemplate;
import com.local_review_deal_sys.dto.LoginFormDTO;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import static com.local_review_deal_sys.utils.RedisConstants.LOGIN_CODE_KEY;

public class EmailRegisterLoginService extends AbstractLoginTemplate {

    private final IService<User> userService;

    public EmailRegisterLoginService(StringRedisTemplate stringRedisTemplate, IService<User> userService) {
        super(stringRedisTemplate);
        this.userService = userService;
    }

    @Override
    protected User authenticate(LoginFormDTO loginForm) {

        String email = loginForm.getEmail();
        String password = loginForm.getPassword();
        String confirmPassword = loginForm.getConfirmPassword();
        String code = loginForm.getCode();

        //  邮箱格式校验
        if (RegexUtils.isEmailInvalid(email)) {
            return null;
        }

        //  是否已注册（注册逻辑）
        if (userService.query().eq("email", email).count() > 0) {
            return null; // 邮箱已注册
        }

        //  密码与确认密码校验
        if (!password.equals(confirmPassword)) {
            return null;
        }

        //  验证码校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + email);
        if (cacheCode == null || !cacheCode.equals(code)) {
            return null;
        }

        //  创建用户
        User user = new User();
        user.setEmail(email);
        user.setPassword(password); // 可以之后加密
        user.setNickName("user_" + System.currentTimeMillis());
        userService.save(user);

        return user;
    }
}
