package com.local_review_deal_sys.observer;


import com.local_review_deal_sys.entity.Blog;

public interface BlogSubject {
    void addObserver(BlogObserver observer);
    void removeObserver(BlogObserver observer);
    void notifyObservers(Blog blog);
}
