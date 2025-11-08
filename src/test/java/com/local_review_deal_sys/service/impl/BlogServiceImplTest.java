package com.local_review_deal_sys.service.impl;//package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.entity.Blog;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.entity.Follow;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.local_review_deal_sys.mapper.BlogMapper;
import com.local_review_deal_sys.service.IFollowService;
import com.local_review_deal_sys.service.IUserService;
import com.local_review_deal_sys.utils.UserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.MockedStatic;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;

import static java.util.List.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.*;


class BlogServiceImplTest {
    private static MockedStatic<UserHolder> mockedUserHolder;


    @InjectMocks
    private BlogServiceImpl blogService;

    @Mock
    private BlogMapper blogMapper;

    @Mock
    private IUserService userService;

    @Mock
    private IFollowService followService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private Blog blog;
    private User user;
    private UserDTO userDTO;

    @BeforeAll
    static void beforeAll() {
        // 用Mockito模拟UserHolder静态方法，并持有静态mock
        mockedUserHolder = Mockito.mockStatic(UserHolder.class);
    }

    @AfterAll
    static void afterAll() {
        // 关闭静态mock
        if (mockedUserHolder != null) {
            mockedUserHolder.close();
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        blog = new Blog();
        blog.setId(1L);
        blog.setUserId(2L);
        blog.setTitle("test blog");

        user = new User();
        user.setId(2L);
        user.setNickName("Joshua");
        user.setIcon("icon.png");

        userDTO = new UserDTO();
        userDTO.setId(10L);
        userDTO.setNickName("Tester");

        // mock静态方法UserHolder.getUser()
        mockedUserHolder.when(UserHolder::getUser).thenReturn(userDTO);

        // mock redis操作
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void testQueryBlogById_found() {
        // mock：查询数据库返回blog
        when(blogMapper.selectById(1L)).thenReturn(blog);
        when(userService.getById(2L)).thenReturn(user);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.score(anyString(), anyString())).thenReturn(null);

        Result result = blogService.queryBlogById(1L);

        assertNotNull(result);
        assertTrue(result.getSuccess());
        Blog data = (Blog) result.getData();
        assertEquals("Joshua", data.getName());
        assertFalse(Boolean.TRUE.equals(data.getIsLike()));
    }

    @Test
    void testQueryBlogById_notFound() {
        when(blogMapper.selectById(999L)).thenReturn(null);
        Result result = blogService.queryBlogById(999L);
        assertFalse(result.getSuccess());
        assertEquals("笔记不存在", result.getErrorMsg());
    }

    @Test
    void testLikeBlog_firstTimeLike() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.score(anyString(), anyString())).thenReturn(null);
        when(blogMapper.update(any(), any())).thenReturn(1); // 模拟更新成功

        Result result = blogService.likeBlog(1L);
        verify(zSetOperations).add(contains("blog:liked"), anyString(), anyDouble());
        assertTrue(result.getSuccess());
    }

    @Test
    void testLikeBlog_cancelLike() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.score(anyString(), anyString())).thenReturn(1.0);
        when(blogMapper.update(any(), any())).thenReturn(1); // 模拟更新成功

        Result result = blogService.likeBlog(1L);
        verify(zSetOperations).remove(contains("blog:liked"), anyString());
        assertTrue(result.getSuccess());
    }

    @Test
    void testQueryHotBlog() {
        // 模拟分页查询返回空的分页结果，避免NPE
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Blog> emptyPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        when(blogMapper.selectPage(any(), any())).thenReturn(emptyPage);
        // 简化：仅验证调用不报错
        assertDoesNotThrow(() -> blogService.queryHotBlog(1));
    }


    @Test
    void testQueryBlogOfFollow_success() {
        // 1️⃣ 模拟 Redis 收件箱
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        ZSetOperations.TypedTuple<String> tuple1 = mock(ZSetOperations.TypedTuple.class);
        when(tuple1.getValue()).thenReturn("1");
        when(tuple1.getScore()).thenReturn((double) System.currentTimeMillis());

        ZSetOperations.TypedTuple<String> tuple2 = mock(ZSetOperations.TypedTuple.class);
        when(tuple2.getValue()).thenReturn("2");
        when(tuple2.getScore()).thenReturn((double) System.currentTimeMillis() - 1000);
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(Arrays.asList(tuple1, tuple2));

        when(zSetOperations.reverseRangeByScoreWithScores(anyString(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(tuples);

        // 2️⃣ 模拟数据库查询两篇 Blog
        Blog blog1 = new Blog();
        blog1.setId(1L);
        blog1.setUserId(2L);

        Blog blog2 = new Blog();
        blog2.setId(2L);
        blog2.setUserId(3L);

        when(blogMapper.selectList(any())).thenReturn(Arrays.asList(blog1, blog2));

        when(userService.getById(2L)).thenReturn(new User() {{
            setNickName("A");
            setIcon("A.png");
        }});
        when(userService.getById(3L)).thenReturn(new User() {{
            setNickName("B");
            setIcon("B.png");
        }});
        when(zSetOperations.score(anyString(), anyString())).thenReturn(null);

        // 3️⃣ 执行方法
        Result result = blogService.queryBlogOfFollow(Long.MAX_VALUE, 0);

        // 4️⃣ 验证返回结果
        System.out.println(result);
        assertTrue(result.getSuccess());
    }
}

