package com.ai.knowledge_base.service;

import com.ai.knowledge_base.exception.DocumentProcessingException;
import com.ai.knowledge_base.exception.UnSupportedContentTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentServiceHelper {

    private final VectorStoreService vectorStoreService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceHelper.class);

    public DocumentServiceHelper(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @Async
    public void addDocument(List<Document> documents){

        logger.info("Asynchronous background worker execution layer activated. Preparing text splitting chunk loop.");

        TextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(900)
                .build();

        List<Document> memoryBatchPool = new ArrayList<>();
        int maxBatchCeiling = 30;

        for(Document document : documents){

            List<Document> chunks = textSplitter.split(List.of(document));
            memoryBatchPool.addAll(chunks);

            if(memoryBatchPool.size() >= maxBatchCeiling){

                logger.info("Flushing micro-batch pool of {} document chunk embedding segments to Vector Store database.", memoryBatchPool.size());
                vectorStoreService.addDocument(memoryBatchPool);
                memoryBatchPool.clear();
            }
        }

        if(!memoryBatchPool.isEmpty()) {

            logger.info("Flushing remaining final micro-batch pool of {} chunks to Vector Store.", memoryBatchPool.size());
            vectorStoreService.addDocument(memoryBatchPool);
        }
    }
}
