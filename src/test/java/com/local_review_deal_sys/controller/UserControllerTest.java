package com.local_review_deal_sys.controller;

import com.local_review_deal_sys.dto.LoginFormDTO;
import com.local_review_deal_sys.dto.PasswordLoginForm;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.entity.UserInfo;
import com.local_review_deal_sys.service.IUserInfoService;
import com.local_review_deal_sys.service.IUserService;
import com.local_review_deal_sys.utils.UserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserController userController;
    private IUserService userService;
    private IUserInfoService userInfoService;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        userService = mock(IUserService.class);
        userInfoService = mock(IUserInfoService.class);
        userController = new UserController();
        // 通过反射注入 Resource 字段
        inject(userController, "userService", userService);
        inject(userController, "userInfoService", userInfoService);
        session = new MockHttpSession();
    }

    // ---------- 测试 /user/code ----------
    @Test
    void testSendCode_success() {
        when(userService.sendCode(eq("test@example.com"), any())).thenReturn(Result.ok("sent"));
        Result result = userController.sendCode("test@example.com", session);
        assertTrue(result.getSuccess());
        verify(userService).sendCode(eq("test@example.com"), any());
    }

    // ---------- 测试 /user/login ----------
    @Test
    void testLogin_success() {
        LoginFormDTO form = new LoginFormDTO();
        form.setEmail("test@example.com");
        when(userService.login(eq(form), any())).thenReturn(Result.ok("login success"));

        Result result = userController.login(form, session);
        assertEquals("login success", result.getData());
        verify(userService).login(eq(form), any());
    }

    // ---------- 测试 /user/loginByPassword ----------
    @Test
    void testLoginByPassword_success() {
        PasswordLoginForm form = new PasswordLoginForm();
        form.setEmail("user@domain.com");
        when(userService.loginByPassword(eq(form), any())).thenReturn(Result.ok("ok"));

        Result result = userController.loginByPassword(form, session);
        assertEquals("ok", result.getData());
        verify(userService).loginByPassword(eq(form), any());
    }

    // ---------- 测试 /user/logout ----------
    @Test
    void testLogout_success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(userService.logout(any())).thenReturn(Result.ok("logged out"));

        Result result = userController.logout(request);
        assertEquals("logged out", result.getData());
        verify(userService).logout(any());
    }

    // ---------- 测试 /user/me ----------
    @Test
    void testMe_returnsCurrentUser() {
        try (MockedStatic<UserHolder> mockedUserHolder = Mockito.mockStatic(UserHolder.class)) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(1L);
            userDTO.setNickName("Joshua");
            mockedUserHolder.when(UserHolder::getUser).thenReturn(userDTO);

            Result result = userController.me();

            assertTrue(result.getSuccess());
            assertEquals("Joshua", ((UserDTO) result.getData()).getNickName());
        }
    }

    // ---------- 测试 /user/info/{id} ----------
    @Test
    void testInfo_success() {
        UserInfo info = new UserInfo();
        info.setUserId(10L);
        info.setGender(false);

        when(userInfoService.getById(10L)).thenReturn(info);

        Result result = userController.info(10L);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        verify(userInfoService).getById(10L);
    }

    @Test
    void testInfo_notFound() {
        when(userInfoService.getById(99L)).thenReturn(null);
        Result result = userController.info(99L);
        assertTrue(result.getSuccess());
        assertNull(result.getData());
    }

    // ---------- 测试 /user/{id} ----------
    @Test
    void testQueryUserById_success() {
        User user = new User();
        user.setId(5L);
        user.setNickName("Tom");

        when(userService.getById(5L)).thenReturn(user);

        Result result = userController.queryUserById(5L);
        assertEquals("Tom", ((UserDTO) result.getData()).getNickName());
        verify(userService).getById(5L);
    }

    @Test
    void testQueryUserById_notFound() {
        when(userService.getById(404L)).thenReturn(null);
        Result result = userController.queryUserById(404L);
        assertNull(result.getData());
    }

    // ---------- 测试 /user/sign ----------
    @Test
    void testSign_success() {
        when(userService.sign()).thenReturn(Result.ok("signed"));
        Result result = userController.sign();
        assertEquals("signed", result.getData());
        verify(userService).sign();
    }

    // ---------- 测试 /user/sign/count ----------
    @Test
    void testSignCount_success() {
        when(userService.signCount()).thenReturn(Result.ok(3));
        Result result = userController.signCount();
        assertEquals(3, result.getData());
        verify(userService).signCount();
    }

    // ---------- 工具方法 ----------
    private void inject(Object target, String field, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}