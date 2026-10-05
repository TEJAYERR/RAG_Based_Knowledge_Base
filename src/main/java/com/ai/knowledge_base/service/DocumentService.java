package com.ai.knowledge_base.service;

import com.ai.knowledge_base.dto.UserResponse;
import com.ai.knowledge_base.exception.ContentMissingException;
import com.ai.knowledge_base.exception.DocumentProcessingException;
import com.ai.knowledge_base.exception.UnSupportedContentTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final VectorStoreService vectorStoreService;
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final DocumentServiceHelper documentServiceHelper;
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    public DocumentService(VectorStoreService vectorStoreService, ChatClient chatClient, ChatMemory chatMemory, DocumentServiceHelper documentServiceHelper) {
        this.vectorStoreService = vectorStoreService;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.documentServiceHelper = documentServiceHelper;
    }

    public void addDocument(MultipartFile multipartFile){

        logger.info("Incoming document upload request received.");

        if(multipartFile.isEmpty()){
            logger.warn("Ingestion aborted: Uploaded multipart file payload is empty.");
            throw new ContentMissingException("no content is provided to process.");
        }

        String type = multipartFile.getContentType();
        String fileName = multipartFile.getOriginalFilename();

        Resource resource = multipartFile.getResource();
        List<Document> documents;

        try {
            switch (type) {
                case "application/pdf" -> {
                    PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
                    documents = reader.get();
                }
                case "text/markdown" -> {
                    MarkdownDocumentReader reader = new MarkdownDocumentReader(
                            resource,
                            MarkdownDocumentReaderConfig.defaultConfig()
                    );
                    documents = reader.get();
                }
                case "text/plain" -> {
                    TextReader reader = new TextReader(resource);
                    documents = reader.get();
                }
                case null, default -> {
                    logger.error("Ingestion failed: Unsupported content type '{}' for file '{}'", type, fileName);
                    throw new UnSupportedContentTypeException("unsupported file type");
                }
            }

            logger.info("Synchronous text extraction complete for file '{}'. Passing {} pages to async worker pool.", fileName, documents.size());
            documentServiceHelper.addDocument(documents);
            logger.info("Handover to background thread execution pool verified successfully.");
        }
        catch (UnSupportedContentTypeException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("Fatal transaction boundary exception hit during document ingestion mapping pipeline.", e);
            throw new DocumentProcessingException("failed to process document!" + e);
        }
    }

    public UserResponse search(String query){
        return this.ask(query);
    }

    public UserResponse ask(String query){

        if(query == null || query.isBlank()){
            throw new ContentMissingException("query missing !");
        }

        logger.info("Retrieving conversation history context for RAG sequence processing.");

        List<Message> history = chatMemory.get("default");
        String searchTargetQuery = query;

        if(history != null && !history.isEmpty()){
            searchTargetQuery = this.reframeQuery(query, history);
        }

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        logger.info("Executing semantic database lookup search targeting keyword string.");
        List<Document> documents = vectorStoreService.search(searchTargetQuery);
        logger.info("Vector store lookup complete. Successfully retrieved {} matching references.", documents.size());

        StringBuilder context = new StringBuilder();
        for (Document doc : documents) {
            context.append("Content: ").append(doc.getText())
                    .append("\nMetadata: ").append(doc.getMetadata()).append("\n\n");
        }

        String systemPrompt = """
                You analyze the provided data context and return the appropriate results.
                            If you cannot answer the question using the context, just say cannot answer.
                
                            Context:
                            {context}
                            ""\"
                """;

        logger.info("Dispatching final system prompt payload execution block to Gemini model.");

        return chatClient.prompt()
                .system(sys -> sys.text(systemPrompt)
                        .param("context", context.toString())
                )
                .user(query)
                .advisors(advisor -> advisor
                        .advisors(messageChatMemoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, "default")
                )
                .call()
                .entity(new ParameterizedTypeReference<UserResponse>() {},
                        ChatClient.EntityParamSpec::validateSchema
                );
    }

    private String reframeQuery(String query, List<Message> history) {

        int maxHistoryWindow = 6;

        String historyText = history.subList(Math.max(0, history.size() - maxHistoryWindow), history.size())
                .stream()
                .map(msg -> msg.getMessageType().name() + ": " + msg.getText())
                .collect(Collectors.joining("\n"));

        String rephrasePrompt = """
                You are an AI search query optimizer. Given the following conversation history and a follow-up question, 
                rewrite the question to be a standalone, fully-descriptive search term for a database.
                - Do NOT answer the question under any circumstances.
                - Return ONLY the raw rephrased search query string. No markdown, no quotes.
                
                History:
                {history}
                """;

        logger.info("Intercepted follow-up conversational question turn. Triggering background query reframer call.");

        String rewrittenQuery = chatClient.prompt()
                .user(query)
                .system(sys -> sys.text(rephrasePrompt)
                        .param("history", historyText)
                )
                .call()
                .content();

        if (rewrittenQuery != null && !rewrittenQuery.isBlank()) {

            logger.info("Successfully optimized query string to standalone search term -> '{}'", rewrittenQuery.trim());
            return rewrittenQuery;
        }

        logger.warn("Query reframer returned blank value. Falling back to original user query text.");
        return query;
    }
}
