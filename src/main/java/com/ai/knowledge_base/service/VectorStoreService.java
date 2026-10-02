package com.ai.knowledge_base.service;

import com.ai.knowledge_base.exception.RetrievalException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreService {

    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addDocument(List<Document> documents){
        vectorStore.add(documents);
    }

    public List<Document> search(String query){
        try {
            return vectorStore.similaritySearch(query);
        }
        catch (Exception e){
            throw new RetrievalException("data retrieval failed try again later.");
        }
    }
}
