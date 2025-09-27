package com.local_review_deal_sys.interceptor;

import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 * @author XiongKun
 * @version 1.0
 */
public class LoginInterceptor implements HandlerInterceptor{

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
        UserHolder.saveUser((User) user);

        //continue
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        //remove the user
        UserHolder.removeUser();
    }
}
