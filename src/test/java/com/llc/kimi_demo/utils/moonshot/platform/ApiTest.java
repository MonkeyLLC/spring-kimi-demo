package com.llc.kimi_demo.utils.moonshot.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llc.kimi_demo.utils.moonshot.platform.util.Message;
import com.llc.kimi_demo.utils.moonshot.platform.util.MoonshotAiUtils;
import com.llc.kimi_demo.utils.moonshot.platform.util.RoleEnum;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class ApiTest {
    private final String userDir = System.getProperty("user.dir");

    @Test
    void getModelList() {
        System.out.println(MoonshotAiUtils.getModelList());
    }

    @Test
    void uploadFile() {
        System.out.println(MoonshotAiUtils.uploadFile(FileUtil.file("/Users/stringle-003/Desktop/store/grobid/test_grobid_in/springer/ai/s10111-024-00765-7.pdf")));
    }

    @Test
    void getFileList() {
        System.out.println(MoonshotAiUtils.getFileList());
    }

    @Test
    void deleteFile() {
        System.out.println(MoonshotAiUtils.deleteFile("co17orilnl9coc91noh0"));
        System.out.println(MoonshotAiUtils.getFileList());
    }

    @Test
    void getFileContent() {
        System.out.println(MoonshotAiUtils.getFileContent("cpshib9kqq4kju87b3q0"));
    }

    @Test
    void getFileDetail() {
        System.out.println(MoonshotAiUtils.getFileDetail("co18sokudu6bc6fqdhhg"));
    }

    @Test
    void estimateTokenCount() {
        List<Message> messages = CollUtil.newArrayList(
                new Message(RoleEnum.system.name(), "你是kimi AI"),
                new Message(RoleEnum.user.name(), "hello")
        );
        System.out.println(MoonshotAiUtils.estimateTokenCount("moonshot-v1-8k", messages));
    }

    @Test
    void chat() {
        List<Message> messages = CollUtil.newArrayList(
                new Message(RoleEnum.system.name(), "你是kimi AI"),
                new Message(RoleEnum.user.name(), "hello")
        );
        MoonshotAiUtils.chat("moonshot-v1-8k", messages);
    }


    @Test
    public void extractAndChat() throws IOException {
        String response = MoonshotAiUtils.uploadFile(
                FileUtil.file(pdfPageExtract("/Users/stringle-003/Desktop/store/grobid/test_grobid_in/springer/ai/s10111-024-00765-7.pdf"))
        );
        String fileId = extractFileId(response);
        if (!StringUtils.hasText(fileId)) {
            return;
        }
        String fileContent = MoonshotAiUtils.getFileContent(fileId);
        System.out.println("File content: " + fileContent);

        // 发送聊天请求
        sendChatRequest(fileContent);
    }

    private void sendChatRequest(String fileContent) throws IOException {
        Message m1 = new Message();
        m1.setRole(RoleEnum.system.name());
        m1.setContent("你是 Kimi，由 Moonshot AI 提供的人工智能助手，你更擅长中文和英文的对话。你会为用户提供安全，有帮助，准确的回答。同时，你会拒绝一切涉及恐怖主义，种族歧视，黄色暴力等问题的回答。Moonshot AI 为专有名词，不可翻译成其他语言。");
        Message m2 = new Message();
        m2.setRole(RoleEnum.system.name());
        m2.setContent(fileContent);
        Message m3 = new Message();
        m3.setRole(RoleEnum.user.name());
        m3.setContent("请简单介绍 s10111-024-00765-7 讲了啥");

        MoonshotAiUtils.chat("moonshot-v1-32k", CollUtil.newArrayList(m1, m2, m3));
    }

    private String extractFileId(String responseBody) throws JsonProcessingException {
        // 解析文件提取响应获取文件 ID
        // 这里根据实际返回的 JSON 结构来解析，假设文件 ID 在 "id" 字段中
        // 例如 {"id": "cpsgvgilnl99ferdis40", ...}
        // 这里可以使用 Gson 或者 Jackson 来更方便地解析 JSON
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        if (null == jsonNode) {
            return null;
        }
        JsonNode idNode = jsonNode.get("id");
        if (null == idNode) {
            return null;
        }
        return idNode.asText();
    }


    public String pdfPageExtract(String sourceFilePath) throws IOException {
        //String sourceFilePath = "/Users/stringle-003/Desktop/store/grobid/test_grobid_in/springer/ai/s10111-024-00765-7.pdf";
        String outputFileDirPath = userDir + "/user_data/pdf/17/";
        Path uploadPath = Paths.get(outputFileDirPath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        try {
            // 加载源 PDF 文件
            PDDocument sourceDocument = PDDocument.load(new File(sourceFilePath));

            // 创建一个新的 PDF 文档来保存前三页
            PDDocument outputDocument = new PDDocument();

            // 获取前三页并添加到新的文档中
            for (int i = 0; i < 3 && i < sourceDocument.getNumberOfPages(); i++) {
                outputDocument.addPage(sourceDocument.getPage(i));
            }

            // 保存新的 PDF 文档
            String filename = FileUtil.getName(sourceFilePath);
            String outputFilePath = outputFileDirPath + "/" + filename;
            outputDocument.save(outputFilePath);

            // 关闭文档
            sourceDocument.close();
            outputDocument.close();
            System.out.println("前三页已成功截取并保存到 " + outputFileDirPath);
            return outputFilePath;


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

}
