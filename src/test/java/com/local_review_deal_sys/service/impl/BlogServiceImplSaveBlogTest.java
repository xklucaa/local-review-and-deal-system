package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.entity.Blog;
import com.local_review_deal_sys.observer.BlogObserver;
import com.local_review_deal_sys.utils.UserHolder;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlogServiceImplSaveBlogTest {

    @InjectMocks
    private BlogServiceImpl blogService;

    @Mock
    private BlogObserver observer1;

    @Mock
    private BlogObserver observer2;

    private Blog blog;
    private UserDTO userDTO;

    private static MockedStatic<UserHolder> mockedUserHolder;

    @BeforeAll
    static void beforeAll() {
        // 静态 Mock
        mockedUserHolder = Mockito.mockStatic(UserHolder.class);
    }

    @AfterAll
    static void afterAll() {
        if (mockedUserHolder != null) {
            mockedUserHolder.close();
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 初始化测试 Blog
        blog = new Blog();
        blog.setId(1L);
        blog.setTitle("Unit Test Blog");

        // 初始化当前用户
        userDTO = new UserDTO();
        userDTO.setId(100L);
        userDTO.setNickName("Tester");
        mockedUserHolder.when(UserHolder::getUser).thenReturn(userDTO);

        // 模拟 ServiceImpl.save() 的行为（父类方法）
        BlogServiceImpl spyService = Mockito.spy(blogService);
        doReturn(true).when(spyService).save(any(Blog.class));

        // 将 spy 替换 injectMocks
        blogService = spyService;

        // 注册两个观察者
        blogService.addObserver(observer1);
        blogService.addObserver(observer2);
    }

    @Test
    void testSaveBlog_success_shouldNotifyAllObservers() {
        // 执行方法
        Result result = blogService.saveBlog(blog);

        // 断言返回结果
        assertTrue(result.getSuccess());
        assertEquals(blog.getId(), result.getData());

        // 验证：UserId 被正确赋值
        assertEquals(100L, blog.getUserId());

        // 验证：所有观察者都被通知
        verify(observer1, times(1)).onBlogPublished(blog);
        verify(observer2, times(1)).onBlogPublished(blog);
    }

    @Test
    void testSaveBlog_fail_shouldReturnError() {
        // mock save 返回 false
        doReturn(false).when(blogService).save(any(Blog.class));

        Result result = blogService.saveBlog(blog);

        assertFalse(result.getSuccess());
        assertEquals("新增笔记失败", result.getErrorMsg());

        // 不应通知观察者
        verify(observer1, never()).onBlogPublished(any());
        verify(observer2, never()).onBlogPublished(any());
    }

    @Test
    void testAddAndRemoveObserver() {
        BlogObserver extraObserver = mock(BlogObserver.class);
        blogService.addObserver(extraObserver);
        blogService.removeObserver(extraObserver);

        blogService.saveBlog(blog);
        verify(extraObserver, never()).onBlogPublished(any());
    }
}
