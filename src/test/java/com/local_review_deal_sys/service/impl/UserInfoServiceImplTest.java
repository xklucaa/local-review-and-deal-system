package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.entity.UserInfo;
import com.local_review_deal_sys.mapper.UserInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserInfoServiceImpl}
 */
class UserInfoServiceImplTest {

    @InjectMocks
    private UserInfoServiceImpl userInfoService;

    @Mock
    private UserInfoMapper userInfoMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------- 测试基础CRUD方法 ----------

    @Test
    void testSaveUserInfo_success() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(1L);
        userInfo.setGender(false);

        when(userInfoMapper.insert(any(UserInfo.class))).thenReturn(1);

        boolean result = userInfoService.save(userInfo);

        assertTrue(result);
        verify(userInfoMapper, times(1)).insert(userInfo);
    }

    @Test
    void testGetById_success() {
        UserInfo user = new UserInfo();
        user.setUserId(2L);


        when(userInfoMapper.selectById(2L)).thenReturn(user);

        UserInfo result = userInfoService.getById(2L);

        assertNotNull(result);
        verify(userInfoMapper).selectById(2L);
    }

    @Test
    void testListAllUsers_success() {
        UserInfo u1 = new UserInfo(); u1.setUserId(1L);
        UserInfo u2 = new UserInfo(); u2.setUserId(2L);

        when(userInfoMapper.selectList(null)).thenReturn(Arrays.asList(u1, u2));

        List<UserInfo> result = userInfoService.list();

        assertEquals(2, result.size());
        verify(userInfoMapper, times(1)).selectList(null);
    }

    @Test
    void testRemoveById_success() {
        when(userInfoMapper.deleteById(3L)).thenReturn(1);

        boolean result = userInfoService.removeById(3L);

        assertTrue(result);
        verify(userInfoMapper).deleteById(3L);
    }

    @Test
    void testSaveUserInfo_failure() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(5L);

        when(userInfoMapper.insert(any(UserInfo.class))).thenReturn(0);

        boolean result = userInfoService.save(userInfo);

        assertFalse(result);
    }
}