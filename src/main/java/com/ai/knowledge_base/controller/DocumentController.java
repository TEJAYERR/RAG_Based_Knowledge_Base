package com.ai.knowledge_base.controller;

import com.ai.knowledge_base.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/documents")
    public void addDocument(@RequestParam MultipartFile multipartFile){
        documentService.addDocument(multipartFile);
    }

    @GetMapping("/documents")
    public ResponseEntity<?> search(@RequestParam String query){
        return new ResponseEntity<>(documentService.search(query), HttpStatus.OK);
    }
}
