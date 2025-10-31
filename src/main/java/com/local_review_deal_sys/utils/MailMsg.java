package com.local_review_deal_sys.utils;
 
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
 
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.Duration;

import static com.local_review_deal_sys.utils.RedisConstants.LOGIN_CODE_KEY;

/**
 * @author hls
 * @date
 * @description: 发送邮箱业务
 */
@Component
public class MailMsg {
 
    @Resource
    private JavaMailSenderImpl mailSender;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
 
    public boolean mail(String email) throws MessagingException {
 
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        //生成随机验证码
        String code = CodeGeneratorUtil.generateCode(6);
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        //设置一个html邮件信息
        helper.setText("<p style='color: blue'>亲爱的用户，欢迎注册本系统！您的验证码为：" + code + "(有效期为两分钟)</p>", true);
        //设置邮件主题名
        helper.setSubject("系统验证码----验证码");
        //发给谁-》邮箱地址
        helper.setTo(email);
        //谁发的-》发送人邮箱
        helper.setFrom("15218705521@163.com");
        //将邮箱验证码以邮件地址为key存入redis,2分钟过期
        redisTemplate.opsForValue().set(LOGIN_CODE_KEY + email, code, Duration.ofMinutes(2));
        mailSender.send(mimeMessage);
        return true;
    }
}