package back.service.impl;

import back.entity.DialogueSessionInfo;
import back.entity.HistoryRecordInfo;
import back.model.*;
import back.repository.DialogueSessionsRepository;
import back.repository.HistoryRecordsRepository;
import back.service.ChatService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import okhttp3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    // @Value("${fwwb.server.address}")
    private final String ollamaHost="127.0.0.1";

    @Value("${ollama.server.port}")
    private int ollamaPort;

    private final String zhipuApiKey="1fc52571d7c3739e6b3a15382f9b300c.AWZdmov1xiwcyPnl";

    private final ObjectMapper objectMapper;
    private final DialogueSessionsRepository dialogueSessionsRepository;
    private final HistoryRecordsRepository historyRecordsRepository;
    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(300);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(600);
    private static final Duration WRITE_TIMEOUT = Duration.ofSeconds(300);
    private static final Duration SSE_TIMEOUT = Duration.ofMinutes(10);

    public ChatServiceImpl(ObjectMapper objectMapper,
                           DialogueSessionsRepository dialogueSessionsRepository,
                           HistoryRecordsRepository historyRecordsRepository) {
        this.objectMapper = objectMapper;
        this.dialogueSessionsRepository = dialogueSessionsRepository;
        this.historyRecordsRepository = historyRecordsRepository;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .writeTimeout(WRITE_TIMEOUT)
                .build();
    }

    @Override
    public Request buildOllamaRequest(ChatRequest requestDTO,boolean stream) throws JsonProcessingException {
        creatHistory(requestDTO);

        // 构建请求体
        List<OllamaMessage> ollamaMessages = new ArrayList<>();

        List<Message> messages = requestDTO.getHistory() != null ? requestDTO.getHistory().getMessages() : new ArrayList<>();
        for (Message message : messages) {
            OllamaMessage ollamaMessage = new OllamaMessage(message.getRole(), message.getContent());
            ollamaMessages.add(ollamaMessage);
        }

        ollamaMessages.add(new OllamaMessage(requestDTO.getQuestion().getRole(),requestDTO.getQuestion().getContent()));

        System.out.println(ollamaMessages);

        OllamaChatRequest request = new OllamaChatRequest(
                requestDTO.getOllama_model(),
                ollamaMessages,
                stream
        );

        return new Request.Builder()
                .url(String.format("http://%s:%d/api/chat", ollamaHost, ollamaPort))
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(request),
                        MediaType.parse("application/json")
                ))
                .build();
    }


    @Override
    public ChatResponse parseResponse(String responseBody, ChatRequest chatRequest) {
        // 直接将响应内容作为普通字符串处理
        Message message = new Message( "model", responseBody, Instant.now());

        // 确保 history 不为 null
        if (chatRequest.getHistory() == null) {
            chatRequest.setHistory(new History());
        }
        History history = chatRequest.getHistory();

        // 构建 ChatResponse
        ChatResponse responseDTO = new ChatResponse();
        responseDTO.setAnswer(message);
        responseDTO.setModel(message.getRole());
        responseDTO.setSessionId(chatRequest.getSessionId());
        responseDTO.setCreatedAt(message.getTimestamp());
        history.getMessages().add(chatRequest.getQuestion());
        history.setSessionId(chatRequest.getSessionId());
        responseDTO.setHistory(history);

        return responseDTO;
    }


    @Override
    public ChatResponse completions(ChatRequest requestDTO) throws IOException {
        if("zhipu".equals(requestDTO.getStatus())) {
            Request request = buildZhipuRequest(requestDTO, false);
            Response response = client.newCall(request).execute();
            ResponseBody responseBodyObj = response.body();
            String responseBody = responseBodyObj.string();
            if (responseBody.isEmpty()) {
                throw new RuntimeException("Response body is empty");
            }
            String extractedContent;
            try {
                JsonNode rootNode = objectMapper.readTree(responseBody);
                // 修复提取逻辑，确保正确提取 choices[0].message.content
                extractedContent = rootNode.path("choices").get(0).path("message").path("content").asText();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse response body", e);
            }

            Message message = new Message("assistant", extractedContent, Instant.now());

            if (requestDTO.getHistory() == null) {
                requestDTO.setHistory(new History());
            }
            History history = requestDTO.getHistory();

            ChatResponse responseDTO = new ChatResponse();
            responseDTO.setAnswer(message);
            responseDTO.setModel(message.getRole());
            responseDTO.setSessionId(requestDTO.getSessionId());
            responseDTO.setCreatedAt(message.getTimestamp());
            history.getMessages().add(requestDTO.getQuestion());
            history.setSessionId(requestDTO.getSessionId());
            responseDTO.setHistory(history);

            return responseDTO;
        }
        else {
            Request request = buildOllamaRequest(requestDTO, false);
            Response response = client.newCall(request).execute();
            System.out.println(response);

            // 解析响应内容并提取嵌套的 content 字段
            String responseBody = response.body().string();
            String extractedContent;
            try {
                JsonNode rootNode = objectMapper.readTree(responseBody);
                extractedContent = rootNode.path("message").path("content").asText();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse response body", e);
            }

            Message message = new Message("model", extractedContent, Instant.now());

            // 确保 history 不为 null
            if (requestDTO.getHistory() == null) {
                requestDTO.setHistory(new History());
            }
            History history = requestDTO.getHistory();

            // 构建 ChatResponse
            ChatResponse responseDTO = new ChatResponse();
            responseDTO.setAnswer(message);
            responseDTO.setModel(message.getRole());
            responseDTO.setSessionId(requestDTO.getSessionId());
            responseDTO.setCreatedAt(message.getTimestamp());
            history.getMessages().add(requestDTO.getQuestion());
            history.setSessionId(requestDTO.getSessionId());
            responseDTO.setHistory(history);

            return responseDTO;
        }
    }

    @Override
    public SseEmitter streamChat(ChatRequest requestDTO) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT.toMillis());
        // 修复异常捕获冲突
        executor.execute(() -> {
            try {
                executeStreamingRequest(emitter, requestDTO);
            } catch (JsonProcessingException e) {
                sendError(emitter, "JSON processing failed: " + e.getMessage());
            } catch (Exception e) {
                sendError(emitter, "Unexpected error: " + e.getMessage());
            }
        });
        return emitter;
    }


    private void executeStreamingRequest(SseEmitter emitter, ChatRequest requestDTO) throws JsonProcessingException {
        Request request = buildOllamaRequest(requestDTO,true);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                System.out.println(body);

                BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
                // 修复流式响应的空指针问题
                if (body == null) {
                    throw new RuntimeException("Response body is null");
                }
                StringBuilder accumulatedContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    OllamaChatResponse chunk = objectMapper.readValue(line, OllamaChatResponse.class);
                    System.out.println(chunk);
                    accumulatedContent.append(chunk.getMessage().getContent());
                    processChunk(emitter, chunk);
                    if (chunk.isDone()) {
                        break;
                    }
                }

                // 在流结束后保存累积的内容到数据库
                String finalContent = accumulatedContent.toString();
                HistoryRecordInfo historyRecordInfo = new HistoryRecordInfo();
                historyRecordInfo.setContent(finalContent);
                historyRecordInfo.setRole("model");
                historyRecordInfo.setTimestamp(Instant.now());
                DialogueSessionInfo dialogueSessionInfo = dialogueSessionsRepository.findById(requestDTO.getSessionId()).orElse(null);
                historyRecordInfo.setSession(dialogueSessionInfo);
                historyRecordsRepository.save(historyRecordInfo);

                ChatResponse chatResponse  = parseResponse(finalContent,requestDTO);
                emitter.send(chatResponse);
                emitter.complete();

            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                sendError(emitter, "Connection failed: " + e.getMessage());
            }
        });
    }
    /**
     * 处理每个数据块
     *
     * @param emitter SseEmitter 对象
     * @param chunk   数据块
     * @throws IOException 处理数据块时发生的异常
     */
    private void processChunk(SseEmitter emitter, OllamaChatResponse chunk) throws IOException {
        ChatStreamResponse chatStreamResponse = new ChatStreamResponse();
        chatStreamResponse.setContent(chunk.getMessage().getContent());
        chatStreamResponse.setDone(chunk.isDone());
        emitter.send(objectMapper.writeValueAsString(chatStreamResponse));
    }


    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(objectMapper.writeValueAsString(
                    new ChatResponse()));
            emitter.completeWithError(new IOException(message));
        } catch (JsonProcessingException e) {
            emitter.completeWithError(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String askModel(String question) throws IOException {
        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "qwen2.5:0.5b");
        requestBody.put("temperature", 0.8);
        requestBody.put("prompt", question);
        requestBody.put("stream", false);

        Request request = new Request.Builder()
                .url(String.format("http://%s:%d/api/generate", ollamaHost, ollamaPort))
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(requestBody),
                        MediaType.parse("application/json")
                ))
                .build();
        Response response = client.newCall(request).execute();


        // 解析响应内容并提取嵌套的 content 字段
        String responseBody = response.body().string();
        System.out.println(responseBody);
        String extractedContent;
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            // 修正为提取 response 字段
            extractedContent = rootNode.path("response").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse response body", e);
        }

        return extractedContent;
    }

    @Override
    public Request buildZhipuRequest(ChatRequest requestDTO, boolean stream) throws JsonProcessingException {
        creatHistory(requestDTO);
        // 构建请求体
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", requestDTO.getZhipu_model());
        requestBody.put("stream", stream);
        // 构建 messages 数组
        List<ObjectNode> messages = requestDTO.getHistory().getMessages().stream()
                .map(message -> {
                    ObjectNode messageNode = objectMapper.createObjectNode();
                    messageNode.put("role", message.getRole());
                    messageNode.put("content", message.getContent());
                    return messageNode;
                })
                .collect(Collectors.toCollection(ArrayList::new)); // 使用可变列表
        System.out.println(messages);
        // 添加用户问题到 messages 数组
        ObjectNode questionNode = objectMapper.createObjectNode();
        questionNode.put("role", requestDTO.getQuestion().getRole());
        questionNode.put("content", requestDTO.getQuestion().getContent());
        messages.add(questionNode);
        requestBody.set("messages", objectMapper.valueToTree(messages));
        requestBody.put("temperature", 0.8);
        return new Request.Builder()
                .url(String.format("https://open.bigmodel.cn/api/paas/v4/chat/completions"))
                .addHeader("Authorization", "Bearer " + zhipuApiKey)
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(requestBody),
                        MediaType.parse("application/json")
                ))
                .build();
    }

    private void creatHistory(ChatRequest requestDTO) {
        // 保存数据
        HistoryRecordInfo historyRecordInfo = new HistoryRecordInfo();
        historyRecordInfo.setContent(requestDTO.getQuestion().getContent());
        historyRecordInfo.setRole(requestDTO.getQuestion().getRole());
        historyRecordInfo.setTimestamp(Instant.now());
        DialogueSessionInfo dialogueSessionInfo = dialogueSessionsRepository.findById(requestDTO.getSessionId()).orElse(null);
        historyRecordInfo.setSession(dialogueSessionInfo);
        historyRecordsRepository.save(historyRecordInfo);
    }
}
