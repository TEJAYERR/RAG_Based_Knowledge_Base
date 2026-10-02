package com.ai.knowledge_base.service;

import com.ai.knowledge_base.dto.UserResponse;
import com.ai.knowledge_base.exception.ContentMissingException;
import com.ai.knowledge_base.exception.DocumentProcessingException;
import com.ai.knowledge_base.exception.UnSupportedContentTypeException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentService {

    private final VectorStoreService vectorStoreService;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public DocumentService(VectorStoreService vectorStoreService, ChatClient chatClient, ChatMemory chatMemory) {
        this.vectorStoreService = vectorStoreService;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    public void addDocument(MultipartFile multipartFile){

        if(multipartFile.isEmpty()){
            throw new ContentMissingException("no content is provided to process.");
        }

        String type = multipartFile.getContentType();
        System.out.println(type);

        Resource resource = multipartFile.getResource();
        List<Document> documents;

        try {

            if ("application/pdf".equals(type)) {
                PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
                documents = reader.get();
            } else if ("text/markdown".equals(type)) {
                MarkdownDocumentReader reader = new MarkdownDocumentReader(
                        resource,
                        MarkdownDocumentReaderConfig.defaultConfig()
                );
                documents = reader.get();
            } else if ("text/plain".equals(type)) {
                TextReader reader = new TextReader(resource);
                documents = reader.get();
            } else {
                throw new UnSupportedContentTypeException("unsupported file type");
            }

            TextSplitter textSplitter = TokenTextSplitter.builder().build();
            List<Document> chunks = textSplitter.split(documents);

            vectorStoreService.addDocument(chunks);
        } catch (UnSupportedContentTypeException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentProcessingException("failed to process document!" + e);
        }
    }

    public UserResponse search(String query){
        return this.ask(query);
    }

    public UserResponse ask(String query){

//        SearchRequest searchRequest = SearchRequest.builder()
//                .query(query)
//                .build();
//
//        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
//                .searchRequest(searchRequest)
//                .build();

        if(query == null || query.isBlank()){
            throw new ContentMissingException("query missing !");
        }

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        List<Document> documents = vectorStoreService.search(query);

        StringBuilder context = new StringBuilder();
        context.append("Question: ")
                .append(query)
                .append("\n\n");

        context.append("Context:\n");

        for(Document doc : documents){
            context.append(doc.getText()).append(doc.getMetadata());
        }

        return chatClient.prompt()
                .user(context.toString())
                .system("you analyzes the data and returns appropriate results, if you cannot answer the question just say cannot answer.")
                .advisors(advisor -> advisor
                        .advisors(messageChatMemoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, "default")
                )
                .call()
                .entity(new ParameterizedTypeReference<UserResponse>() {});
    }
}
