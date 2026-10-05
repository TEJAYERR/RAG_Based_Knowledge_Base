package com.ai.knowledge_base.exception;


//No need spring AI abstraction already provides Transient and nontransient exceptions to handle.

//deprecated my own class
public class AIFailureException extends RuntimeException {

    public AIFailureException(String message){
        super(message);
    }
}
