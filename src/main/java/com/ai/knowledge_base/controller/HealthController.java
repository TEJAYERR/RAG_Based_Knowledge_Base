package com.ai.knowledge_base.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
public class HealthController {

    @GetMapping("health")
    public ResponseEntity<?> health(){
        return ResponseEntity.ok("working");
    }
}
