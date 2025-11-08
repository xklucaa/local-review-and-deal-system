package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.dto.LoginFormDTO;
import com.local_review_deal_sys.dto.PasswordLoginForm;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.mapper.UserMapper;
import com.local_review_deal_sys.utils.MailMsg;
import com.local_review_deal_sys.utils.PasswordTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}
 */
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MailMsg mailMsg;
    @Mock
    private ShopServiceImpl shopService;
    @Mock
    private HttpSession session;
    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ------------------ sendCode ------------------

    @Test
    void testSendCode_success() throws Exception {
        String email = "test@example.com";
        when(valueOperations.get(LOGIN_CODE_KEY + email)).thenReturn(null);
        when(mailMsg.mail(email)).thenReturn(true);

        Result result = userService.sendCode(email, session);

        assertTrue(result.getSuccess());
        verify(mailMsg).mail(email);
    }

    @Test
    void testSendCode_invalidEmail() {
        Result result = userService.sendCode("bademail", session);

        assertFalse(result.getSuccess());
        assertEquals("Invalid email number", result.getErrorMsg());
    }

    @Test
    void testSendCode_mailException() throws Exception {
        String email = "user@example.com";
        when(valueOperations.get(LOGIN_CODE_KEY + email)).thenReturn(null);
        when(mailMsg.mail(email)).thenThrow(new MessagingException("SMTP error"));

        Result result = userService.sendCode(email, session);

        assertFalse(result.getSuccess());
        assertEquals("验证码发送失败", result.getErrorMsg());
    }

    // ------------------ login (register new user) ------------------

   @Test
    void testLogin_registerNewUser_success() {
        LoginFormDTO form = new LoginFormDTO();
        form.setEmail("new@example.com");
        form.setPassword("123456");
        form.setConfirmPassword("123456");
        form.setCode("8888");

        // 模拟数据库中没有该用户，触发注册流程
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(valueOperations.get(LOGIN_CODE_KEY + "new@example.com")).thenReturn("8888");
        when(userMapper.insert(any(User.class))).thenReturn(1); // 插入成功

        Result result = userService.login(form, session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        verify(stringRedisTemplate).expire(startsWith(LOGIN_USER_KEY), eq(LOGIN_USER_TTL), eq(TimeUnit.SECONDS));
    }


    @Test
    void testLogin_passwordMismatch() {
        LoginFormDTO form = new LoginFormDTO();
        form.setEmail("a@b.com");
        form.setPassword("123");
        form.setConfirmPassword("456");

        Result result = userService.login(form, session);

        assertFalse(result.getSuccess());
        assertEquals("Passwords do not match", result.getErrorMsg());
    }

    @Test
    void testLogin_invalidCode() {
        LoginFormDTO form = new LoginFormDTO();
        form.setEmail("a@b.com");
        form.setPassword("123456");
        form.setConfirmPassword("123456");
        form.setCode("9999");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOGIN_CODE_KEY + "a@b.com")).thenReturn("0000");

        Result result = userService.login(form, session);

        assertFalse(result.getSuccess());
        assertEquals("Invalid verification code", result.getErrorMsg());
    }

    // ------------------ loginByPassword ------------------

    @Test
    void testLoginByPassword_success() {
        PasswordLoginForm form = new PasswordLoginForm();
        form.setEmail("user@example.com");
        form.setPassword("123456");

        User dbUser = new User();
        dbUser.setId(1L);
        dbUser.setEmail("user@example.com");
        dbUser.setPassword(PasswordTools.encrypt("123456"));

        doReturn(mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class, RETURNS_DEEP_STUBS))
                .when(userService).query();
        when(userMapper.selectList(any())).thenReturn(Collections.singletonList(dbUser));
        when(userMapper.selectOne(any())).thenReturn(dbUser);
        when(userService.getById(1L)).thenReturn(dbUser);

        when(userMapper.selectById(anyLong())).thenReturn(dbUser);
        when(userMapper.selectOne(any())).thenReturn(dbUser);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Result result = userService.loginByPassword(form, session);

        assertTrue(result.getSuccess());
        verify(stringRedisTemplate).expire(startsWith(LOGIN_USER_KEY), eq(LOGIN_USER_TTL), eq(TimeUnit.SECONDS));
    }

    @Test
    void testLoginByPassword_wrongPassword() {
        PasswordLoginForm form = new PasswordLoginForm();
        form.setEmail("user@example.com");
        form.setPassword("wrong");

        User dbUser = new User();
        dbUser.setEmail("user@example.com");
        dbUser.setPassword(PasswordTools.encrypt("123456"));

        doReturn(mock(com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class, RETURNS_DEEP_STUBS))
    .when(userService).query();
        when(userMapper.selectOne(any())).thenReturn(dbUser);

        Result result = userService.loginByPassword(form, session);

        assertFalse(result.getSuccess());
        assertEquals("密码错误", result.getErrorMsg());
    }

    // ------------------ logout ------------------

    @Test
    void testLogout_success() {
        when(request.getHeader("authorization")).thenReturn("abc-token");

        Result result = userService.logout(request);

        assertTrue(result.getSuccess());
        verify(stringRedisTemplate).delete(LOGIN_USER_KEY + "abc-token");
    }

    @Test
    void testLogout_noToken() {
        when(request.getHeader("authorization")).thenReturn(null);

        Result result = userService.logout(request);

        assertTrue(result.getSuccess());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    // ------------------ loadShopData ------------------

    @Test
    void testLoadShopData_addsToRedisGeo() {
        Shop s1 = new Shop();
        s1.setId(1L);
        s1.setTypeId(10L);
        s1.setX(120.1);
        s1.setY(30.2);

        when(shopService.list()).thenReturn(Collections.singletonList(s1));
        when(stringRedisTemplate.opsForGeo()).thenReturn(mock(org.springframework.data.redis.core.GeoOperations.class));

        userService.loadShopData();

        verify(shopService).list();
    }
}