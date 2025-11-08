package com.local_review_deal_sys.config;

import com.local_review_deal_sys.observer.FollowFeedObserver;
import com.local_review_deal_sys.service.impl.BlogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class BlogObserverConfig {

    @Autowired
    private BlogServiceImpl blogService;

    @Autowired
    private FollowFeedObserver followFeedObserver;

    @PostConstruct
    public void init() {
        // 注册观察者
        blogService.addObserver(followFeedObserver);
    }
}
