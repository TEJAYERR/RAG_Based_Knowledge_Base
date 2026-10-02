package com.ai.knowledge_base.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserResponse {

    String answer;
    List<String> sources;
}
