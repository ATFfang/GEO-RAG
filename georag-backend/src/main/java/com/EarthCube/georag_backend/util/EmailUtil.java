package com.EarthCube.georag_backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmailUtil {

    @Resource
    private JavaMailSender mailSender;

    // 读取配置文件中的发件人
    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送纯文本邮件
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     */
    public void sendTextEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件已发送至: {}", to);
        } catch (Exception e) {
            log.error("发送邮件失败: ", e);
            throw new RuntimeException("邮件服务异常");
        }
    }
}