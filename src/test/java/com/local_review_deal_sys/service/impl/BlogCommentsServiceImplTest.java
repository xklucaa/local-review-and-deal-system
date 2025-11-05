package com.local_review_deal_sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.local_review_deal_sys.entity.BlogComments;
import com.local_review_deal_sys.mapper.BlogCommentsMapper;
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
 * Unit test for BlogCommentsServiceImpl
 */
class BlogCommentsServiceImplTest {

    @InjectMocks
    private BlogCommentsServiceImpl blogCommentsService;

    @Mock
    private BlogCommentsMapper blogCommentsMapper;

    private BlogComments comment;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        comment = new BlogComments();
        comment.setId(1L);
        comment.setBlogId(10L);
        comment.setUserId(5L);
        comment.setContent("Nice post!");
    }

    @Test
    void testSaveComment_success() {
        when(blogCommentsMapper.insert(any(BlogComments.class))).thenReturn(1);

        boolean saved = blogCommentsService.save(comment);

        assertTrue(saved);
        verify(blogCommentsMapper, times(1)).insert(any(BlogComments.class));
    }

    @Test
    void testGetById_success() {
        when(blogCommentsMapper.selectById(1L)).thenReturn(comment);

        BlogComments result = blogCommentsService.getById(1L);

        assertNotNull(result);
        assertEquals("Nice post!", result.getContent());
        verify(blogCommentsMapper).selectById(1L);
    }

    @Test
    void testListComments() {
        when(blogCommentsMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(comment));

        List<BlogComments> comments = blogCommentsService.list();

        assertEquals(1, comments.size());
        assertEquals(10L, comments.get(0).getBlogId());
        verify(blogCommentsMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void testRemoveById_success() {
        when(blogCommentsMapper.deleteById(1L)).thenReturn(1);

        boolean removed = blogCommentsService.removeById(1L);

        assertTrue(removed);
        verify(blogCommentsMapper).deleteById(1L);
    }
}