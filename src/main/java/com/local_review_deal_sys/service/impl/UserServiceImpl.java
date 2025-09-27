package com.local_review_deal_sys.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.mapper.UserMapper;
import com.local_review_deal_sys.service.IUserService;
import com.local_review_deal_sys.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.local_review_deal_sys.dto.LoginFormDTO;

import javax.servlet.http.HttpSession;

import java.util.Collections;

import static com.local_review_deal_sys.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //check the phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            //if phone number is invalid, return a failure message
            return Result.fail("Invalid phone number");
        }

        //if phone number is valid, generate a verification code
        String code = RandomUtil.randomNumbers(6);

        //store the code in session
        session.setAttribute("code", code);
        // 在 login 方法开始处添加

        log.debug("Session ID: {}", session.getId());
        log.debug("All session attributes: {}", Collections.list(session.getAttributeNames()));

        //send the code to the phone number

        //暂时是用模拟的来做的，还需要继续完善

        log.debug("Send verification code successfully, code is {}", code);
        return Result.ok();
    }


    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //1. check the phone number(防止用户收到验证码后又自己换了个手机号)
        if (RegexUtils.isPhoneInvalid(phone)) {
            //if phone number is invalid, return a failure message
            return Result.fail("Invalid phone number");
        }

        //2. check the verification code
        Object cacheCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            //3. if not true, then return a failure message
            return Result.fail("Invalid verification code");
        }

        //4. if true, then query the user by the phone number
        User user = query().eq("phone", phone).one();

        //5. if the user does not exist, then create a new user and store it to DB and session
        if(user == null){
            user = createUserWithPhone(phone);
        }
        //6. if the user exists, then store the user to session
        session.setAttribute("user", user);

        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        //create user
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX  + RandomUtil.randomString(10));
        //save the suer to DB
        save(user);
        return user;
    }

}
