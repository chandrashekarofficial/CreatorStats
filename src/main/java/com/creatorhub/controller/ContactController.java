package com.creatorhub.controller;

import com.creatorhub.dto.ContactRequest;
import com.creatorhub.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody ContactRequest request) {

        if (request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getSubject() == null || request.getSubject().isBlank()
                || request.getMessage() == null || request.getMessage().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Please complete all fields.");
        }

        try {
            contactService.sendContactMessage(request);
            return ResponseEntity.ok("Message sent successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Unable to send message right now.");
        }
    }
}
