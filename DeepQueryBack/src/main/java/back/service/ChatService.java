package back.service;


import java.io.IOException;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.core.JsonProcessingException;

import back.model.ChatRequest;
import back.model.ChatResponse;
import okhttp3.Request;


public interface ChatService {

    Request buildOllamaRequest(ChatRequest requestDTO,boolean stream) throws JsonProcessingException;
    
    Request buildZhipuRequest(ChatRequest requestDTO,boolean stream) throws JsonProcessingException;

    ChatResponse parseResponse(String responseBody,ChatRequest chatRequest) throws JsonProcessingException;

    ChatResponse completions(ChatRequest requestDTO) throws IOException;

    SseEmitter streamChat(ChatRequest requestDTO);

    /**
     * 发送问题给大模型并返回答复内容（只返回答复字符串，不做其他处理）
     */
    String askModel(String question) throws IOException;

}
