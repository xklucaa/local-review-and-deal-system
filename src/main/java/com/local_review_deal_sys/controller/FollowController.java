package com.local_review_deal_sys.controller;


import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.dto.UserDTO;
import com.local_review_deal_sys.service.IFollowService;
import com.local_review_deal_sys.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,
                             @PathVariable("isFollow") Boolean isFollow) {
        // 关注或取关
        return followService.follow(followUserId, isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId) {
        // 查询是否关注
        return followService.isFollow(followUserId);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id) {
        return followService.followCommons(id);
    }
}
