package com.creatorhub.service;

import com.creatorhub.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String recipient;

    public ContactService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactMessage(ContactRequest request) {

        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setTo(recipient);
        mail.setReplyTo(request.getEmail());
        mail.setSubject("CreatorStats Contact: " + request.getSubject());

        mail.setText(
            "New message from CreatorStats website\n\n" +
            "Name: " + request.getName() + "\n" +
            "Email: " + request.getEmail() + "\n" +
            "Subject: " + request.getSubject() + "\n\n" +
            "Message:\n" +
            request.getMessage()
        );

        mailSender.send(mail);
    }
}
