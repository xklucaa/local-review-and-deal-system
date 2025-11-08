package com.local_review_deal_sys.service.login;

import com.local_review_deal_sys.abstractLogin.AbstractLoginTemplate;
import com.local_review_deal_sys.dto.PasswordLoginForm;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.utils.PasswordTools;
import com.local_review_deal_sys.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

public class PasswordLoginService extends AbstractLoginTemplate {

    private final IService<User> userService;

    public PasswordLoginService(StringRedisTemplate stringRedisTemplate, IService<User> userService) {
        super(stringRedisTemplate);
        this.userService = userService;
    }

    @Override
    protected User authenticate(Object formObj) { // ★ 接受登录表单
        PasswordLoginForm form = (PasswordLoginForm) formObj;

        String email = form.getEmail();
        String password = form.getPassword();

        // ① 邮箱格式校验
        if (RegexUtils.isEmailInvalid(email)) {
            return null;
        }

        // ② 查询用户
        User user = userService.query().eq("email", email).one();
        if (user == null) {
            return null; // 用户不存在
        }

        // ③ 密码比对
        if (!PasswordTools.decrypt(password, user.getPassword())) {
            return null; // 密码不正确
        }

        return user; // ✅ 返回用户，父类负责写入 Redis + 返回 token
    }
}
