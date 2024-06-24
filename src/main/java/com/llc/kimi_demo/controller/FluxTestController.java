package com.llc.kimi_demo.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.llc.kimi_demo.utils.moonshot.platform.util.Message;
import com.llc.kimi_demo.utils.moonshot.platform.util.RoleEnum;
import okhttp3.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/flux")
public class FluxTestController {

    private static final String API_KEY = "sk-tV0MOcPRpNMyyDRH6aDdSsH9z5wsb9pgs5hQHP2efBpbIhgy";
    private static final String MODELS_URL = "https://api.moonshot.cn/v1/models";
    private static final String FILES_URL = "https://api.moonshot.cn/v1/files";
    private static final String ESTIMATE_TOKEN_COUNT_URL = "https://api.moonshot.cn/v1/tokenizers/estimate-token-count";
    private static final String CHAT_COMPLETION_URL = "https://api.moonshot.cn/v1/chat/completions";

    @GetMapping(value = "/chat", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> test(@RequestParam String model) {
        System.out.print("开始处理");
        List<Message> messages = CollUtil.newArrayList(
                new Message(RoleEnum.system.name(), "你是 peak，由 李利超开发的人工智能助手，你更擅长中文和英文的对话。你会为用户提供安全，有帮助，准确的回答。同时，你会拒绝一切涉及恐怖主义，种族歧视，黄色暴力等问题的回答。李利超为专有名词，不可翻译成其他语言。"),
                new Message(RoleEnum.user.name(), "hello"),
                new Message(RoleEnum.system.name(), "我想要翻译请你可以帮我吗")
        );
        return Flux.create(sink -> {
            try {
                String requestBody = new JSONObject()
                        .putOpt("model", model)
                        .putOpt("messages", messages)
                        .putOpt("stream", true)
                        .toString();
                Request okhttpRequest = new Request.Builder()
                        .url(CHAT_COMPLETION_URL)
                        .post(RequestBody.create(requestBody, MediaType.get(ContentType.JSON.getValue())))
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .build();
                Call call = new OkHttpClient().newCall(okhttpRequest);
                Response okhttpResponse = call.execute();
                assert okhttpResponse.body() != null;
                BufferedReader reader = new BufferedReader(okhttpResponse.body().charStream());
                String line;
                while ((line = reader.readLine()) != null) {
                    if (StrUtil.isBlank(line)) {
                        continue;
                    }
                    if (JSONUtil.isTypeJSON(line)) {
                        Optional.of(JSONUtil.parseObj(line))
                                .map(x -> x.getJSONObject("error"))
                                .map(x -> x.getStr("message"))
                                .ifPresent(x -> {
                                    sink.next("error: " + x);
                                    sink.complete();
                                });
                        return;
                    }
                    line = StrUtil.replace(line, "data: ", StrUtil.EMPTY);
                    if (StrUtil.equals("[DONE]", line) || !JSONUtil.isTypeJSON(line)) {
                        sink.complete();
                        return;
                    }
                    Optional.of(JSONUtil.parseObj(line))
                            .map(x -> x.getJSONArray("choices"))
                            .filter(CollUtil::isNotEmpty)
                            .map(x -> (JSONObject) x.get(0))
                            .map(x -> x.getJSONObject("delta"))
                            .map(x -> x.getStr("content"))
                            .ifPresent(sink::next);
                }
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }
}
