package com.summer.devopsplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summer.devopsplatform.config.JenkinsConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class JenkinsServiceImpl implements JenkinsService {
    private final JenkinsConfig jenkinsConfig;
    private final ObjectMapper objectMapper;

    public JenkinsServiceImpl(JenkinsConfig jenkinsConfig, ObjectMapper objectMapper) {
        this.jenkinsConfig = jenkinsConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public String triggerJob(String jobName) {
        log.info("trigger job: {}", jobName);

        // 1. 拼装出完整的URL
        String url = jenkinsConfig.getUrl() + "/job/" + jobName + "/build";
        log.info("url: {}", url);

        // 2. 创建HTTP客户端
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 3. 获取CSRF crumb
            Crumb crumb = getCrumb(httpClient);
            if (crumb == null) {
                log.warn("Failed to get CSRF crumb, proceeding without it");
            }

            // 4. 创建POST请求
            HttpPost httpPost = new HttpPost(url);

            // 5. 添加基本认证头
            String authHeader = createAuthHeader(jenkinsConfig.getUsername(), jenkinsConfig.getApitoken());
            httpPost.addHeader("Authorization", authHeader);

            // 6. 添加CSRF crumb头（如果获取成功）
            if (crumb != null) {
                httpPost.addHeader(crumb.getCrumbRequestField(), crumb.getCrumb());
                log.info("Added CSRF protection header: {} = {}", crumb.getCrumbRequestField(), crumb.getCrumb());
            }

            log.info("正在向url {}发送请求", url);

            // 7. 执行请求并获取响应
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();

            // 8. 根据请求状态判断成功与否
            if (statusCode == 201) {
                log.info("请求成功");
                log.info("成功触发job {}", jobName);
                return "成功触发job " + jobName;
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.error("请求失败,状态码为{}, 响应内容: {}", statusCode, responseBody);
                return "请求失败,状态码为 " + statusCode + ", 响应内容: " + responseBody;
            }
        } catch (IOException e) {
            log.error("请求失败", e);
            return "请求失败,发生了网络错误: " + e.getMessage();
        }
    }

    // 获取jenkins CSRF crumb
    private Crumb getCrumb(CloseableHttpClient httpClient) {
        try {
            String crumbUrl = jenkinsConfig.getUrl() + "/crumbIssuer/api/json";
            HttpGet httpGet = new HttpGet(crumbUrl);

            // 添加基本认证头
            String authHeader = createAuthHeader(jenkinsConfig.getUsername(), jenkinsConfig.getApitoken());
            httpGet.addHeader("Authorization", authHeader);

            HttpResponse response = httpClient.execute(httpGet);
            log.info("crumb response status: {}", response.getStatusLine().getStatusCode());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String jsonResponse = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.debug("Crumb API response: {}", jsonResponse);

                JsonNode jsonNode = objectMapper.readTree(jsonResponse);
                String crumb = jsonNode.get("crumb").asText();
                String crumbRequestField = jsonNode.get("crumbRequestField").asText();

                log.info("Successfully retrieved CSRF crumb: {} = {}", crumbRequestField, crumb);
                return new Crumb(crumb, crumbRequestField);
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.warn("Failed to get CSRF crumb, status code: {}, response: {}", statusCode, responseBody);
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to get CSRF crumb: {}", e.getMessage());
            return null;
        }
    }

    // 创建基本认证头
    private String createAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }

    /*
    内部存储crumb的类
     */
    private static class Crumb {
        private final String crumb;
        private final String crumbRequestField;

        public Crumb(String crumb, String crumbRequestField) {
            this.crumb = crumb;
            this.crumbRequestField = crumbRequestField;
        }

        public String getCrumb() {
            return crumb;
        }

        public String getCrumbRequestField() {
            return crumbRequestField;
        }
    }
}