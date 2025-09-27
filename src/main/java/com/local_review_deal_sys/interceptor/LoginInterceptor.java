package com.local_review_deal_sys.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.LOGIN_USER_KEY;
import static com.local_review_deal_sys.utils.RedisConstants.LOGIN_USER_TTL;


/**
 * @author XiongKun
 * @version 1.0
 */
public class LoginInterceptor implements HandlerInterceptor{

    private StringRedisTemplate stringRedisTemplate;
    //此时不能用一些注解来实现自动注入，需要我们手动创建对象。因为这个interceptor不是component

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 直接放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }


        /*
        //1. get session
        HttpSession session = request.getSession();
        //2. get the user from the session
        Object user = session.getAttribute("user");
        //3. user exits? (401 == unathorized)
        if(user == null){
            //if not exists, then intercept it
            response.setStatus(401);
            return false;
        }

        //if exists, then store the user in ThreadLocal
        UserHolder.saveUser((UserDTO) user);

        //continue
        return true;

         */

        //1. 获取请求头的token
        String token = request.getHeader("authorization");
        //2. 基于token获取redis用户
        if(StrUtil.isBlank(token)) {
            //不存在，拦截，返回401
            System.out.println("Token is blank, returning 401");
            response.setStatus(401);
            return false;
        }

        //3. 判断用户是否存在
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        if(userMap.isEmpty()){
            //4.不存在，拦截，返回401
            System.out.println("user is not exist, returning 401");
            response.setStatus(401);
            return false;
        }



        //5.将查询到的hash值转为userDTO  ==> 因为用的是hash存入的值
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //6. 存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);
        //7. 刷新token有效期
        // ==> 如果不设置，则只要我们规定的时间就会过期，而我们需要如果再次发起请求，可以刷新有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);
        //8. 放行

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        //remove the user
        UserHolder.removeUser();
    }
}
