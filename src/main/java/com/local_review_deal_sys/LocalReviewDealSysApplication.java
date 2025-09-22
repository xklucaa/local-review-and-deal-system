package com.local_review_deal_sys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.local_review_deal_sys.mapper")
@SpringBootApplication
public class LocalReviewDealSysApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalReviewDealSysApplication.class, args);
    }

}
