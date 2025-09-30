package com.local_review_deal_sys.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.mapper.UserMapper;
import com.local_review_deal_sys.service.IUserService;
import com.local_review_deal_sys.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.local_review_deal_sys.dto.LoginFormDTO;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.*;
import static com.local_review_deal_sys.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendCode(String phone, HttpSession session) {
        //check the phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            //if phone number is invalid, return a failure message
            return Result.fail("Invalid phone number");
        }

        //if phone number is valid, generate a verification code
        String code = RandomUtil.randomNumbers(6);

        /*
        //store the code in session
        session.setAttribute("code", code);
         */
        //now store the code in redis, key is phone number, value is code
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL,TimeUnit.MINUTES);

        /*
        just for test, the session get from frontend is different at first. (solved)
        log.debug("Session ID: {}", session.getId());
        log.debug("All session attributes: {}", Collections.list(session.getAttributeNames()));
         */


        //send the code to the phone number

        //暂时是用模拟的来做的，还需要继续完善,试试看发到邮箱

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

        //2. check the verification code, now we get it from redis

        /*get the code from session
        Object cacheCode = session.getAttribute("code");

         */
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            //3. if not true, then return a failure message
            return Result.fail("Invalid verification code");
        }

        //4. if true, then query the user by the phone number
        User user = query().eq("phone", phone).one();


        // 5. if the user does not exist, then create a new user and store it to DB

        if(user == null){
            user = createUserWithPhone(phone);
        }

        //6. then store the user to redis

        //6.1 generate a token for login  ==>key
        String token = UUID.randomUUID().toString(true);
        //6.2 transfer the user to hash to store  ==>value
        // 修改为以下代码
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, value) -> value == null ? null : value.toString()));

        //6.3 store it to redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,userMap);

        //6.4 set token expire time
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL,TimeUnit.SECONDS);


//        //5. if the user does not exist, then create a new user and store it to DB and session
//        if(user == null){
//            user = createUserWithPhone(phone);
//        }
//        //6. if the user exists, then store the user to session
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        //6.4 return the token to the client side
        return Result.ok(token);
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
