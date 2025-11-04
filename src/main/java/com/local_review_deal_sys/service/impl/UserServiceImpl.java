package com.local_review_deal_sys.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.local_review_deal_sys.dto.PasswordLoginForm;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.mapper.UserMapper;
import com.local_review_deal_sys.service.IUserService;
import com.local_review_deal_sys.utils.MailMsg;
import com.local_review_deal_sys.utils.PasswordTools;
import com.local_review_deal_sys.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.mapper.UserMapper;
import com.local_review_deal_sys.service.IUserService;
import com.local_review_deal_sys.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.local_review_deal_sys.dto.LoginFormDTO;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.*;
import static com.local_review_deal_sys.utils.SystemConstants.USER_NICK_NAME_PREFIX;


import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static com.local_review_deal_sys.utils.RedisConstants.USER_SIGN_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MailMsg mailMsg;
    @Resource
    private ShopServiceImpl shopService;


    @Override
    public Result sendCode (String email, HttpSession session) {
        //check the email number
        if (RegexUtils.isEmailInvalid(email)) {
            //if phone email is invalid, return a failure message
            return Result.fail("Invalid email number");
        }
        log.info("邮箱码：{}",email);
        //从redis中取出验证码信息
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + email);
        if (!StringUtils.isEmpty(code)) {
            return Result.ok();
        }
        try {
            boolean b = mailMsg.mail(email);
            if (b) {
                return Result.ok();
            }
            return Result.fail("邮箱不正确或为空");
        } catch (MessagingException e) {
            log.error("发送邮件验证码失败", e);
            return Result.fail("验证码发送失败");
        }
    }


    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String email = loginForm.getEmail();
        String password = loginForm.getPassword();
        String confirmPassword = loginForm.getConfirmPassword();
        //1. check the email number
        if (RegexUtils.isEmailInvalid(email)) {
            //if email number is invalid, return a failure message
            return Result.fail("Invalid email number");
        }

        // check if the email has been registered
        if (query().eq("email", email).count() > 0) {
            return Result.fail("Email already registered");
        }

        // check if the passwords equals confirmPasswords
        if (!password.equals(confirmPassword)) {
            return Result.fail("Passwords do not match");
        }


        // check the verification code, now we get it from redis
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + email);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            // if not true, then return a failure message
            return Result.fail("Invalid verification code");
        }

        // if true, then query the user by the email number
        User user = query().eq("email", email).one();

        // if the user does not exist, then create a new user and store it to DB

        if(user == null){
            user = createUserWithEmail(email, password);
            //save the suer to DB
            save(user);
        }
        // then store the user to redis
        // generate a token for login  ==>key
        String token = UUID.randomUUID().toString(true);
        // transfer the user to hash to store  ==>value
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, value) -> value == null ? null : value.toString()));

        //6.3 store it to redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,userMap);

        //6.4 set token expire time
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL,TimeUnit.SECONDS);
        loadShopData();
        //6.4 return the token to the client side
        return Result.ok(token);
    }

    @Override
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        System.out.println("token: " + token);
        if(token != null){
            stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        }
        return Result.ok();
    }

    @Override
    public Result loginByPassword(PasswordLoginForm loginForm, HttpSession session) {
        String email = loginForm.getEmail();
        String password = loginForm.getPassword();

        //1. check the email number
        if (RegexUtils.isEmailInvalid(email)) {
            //if email number is invalid, return a failure message
            return Result.fail("Invalid email number");
        }

        // 2. check if the email has been registered
        User user = query().eq("email", email).one();
        if (user == null) {
            return Result.fail("Email has not been registered");
        }

        // 3. check if the passwords equals the one in DB
        if (!PasswordTools.decrypt(password, user.getPassword())) { // 或者使用加密校验逻辑
            return Result.fail("密码错误");
        }

        String token = UUID.randomUUID().toString(true);
        // transfer the user to hash to store  ==>value
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, value) -> value == null ? null : value.toString()));

        //6.3 store it to redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,userMap);

        //6.4 set token expire time
        stringRedisTemplate.expire(LOGIN_USER_KEY + token,LOGIN_USER_TTL,TimeUnit.SECONDS);
        loadShopData();
        //6.4 return the token to the client side
        return Result.ok(token);
    }


    private User createUserWithEmail(String email, String password) {
        //create user
        User user = new User();
        user.setEmail(email);
        // 对密码进行加密存储
        user.setPassword(PasswordTools.encrypt(password));
        user.setNickName(USER_NICK_NAME_PREFIX  + RandomUtil.randomString(10));
        return user;
    }



    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return Result.ok(0);
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            } else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }

    void loadShopData() {
        // 1.查询店铺信息
        List<Shop> list = shopService.list();
        // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 3.分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 3.1.获取类型id
            Long typeId = entry.getKey();
            String key = "shop:geo:" + typeId;
            // 3.2.获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            // 3.3.写入redis GEOADD key 经度 纬度 member
            for (Shop shop : value) {
                // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
