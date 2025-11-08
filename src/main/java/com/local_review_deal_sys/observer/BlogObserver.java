package com.local_review_deal_sys.observer;

import com.local_review_deal_sys.entity.Blog;


public interface BlogObserver {
    void onBlogPublished(Blog blog);
}
