package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.entity.SeckillVoucher;
import com.local_review_deal_sys.mapper.SeckillVoucherMapper;
import com.local_review_deal_sys.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
