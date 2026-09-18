package com.example.picturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.example.picturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.example.picturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.example.picturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliyunAIApi {

    @Value("${aliyun.bailian.app-key}")
    private String appKey;

    @Value("${aliyun.bailian.workspace-id}")
    private String workspaceId;

    // 创建任务请求地址
    public static final String CREATE_TASK_URL = "https://%s.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 获取任务状态请求地址
    public static final String GET_TASK_STATUS_URL = "https://%s.cn-beijing.maas.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建外扩图任务
     *
     * @param createOutPaintingTaskRequest 创建外扩图任务请求参数
     *
     * @return 创建外扩图任务响应结果
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        // 1. 校验参数
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 2. 调用阿里云接口创建任务
        HttpRequest httpRequest = HttpRequest.post(String.format(CREATE_TASK_URL, workspaceId))
                .header("Authorization", "Bearer " + appKey)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        try (HttpResponse response = httpRequest.execute()) {
            if (!response.isOk()) {
                log.error("调用阿里云接口创建任务失败，状态码：{}，响应体：{}", response.getStatus(), response.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用阿里云接口创建任务失败，状态码：" + response.getStatus());
            }

            CreateOutPaintingTaskResponse taskResponse = JSONUtil.toBean(response.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = taskResponse.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String responseMessage = taskResponse.getMessage();
                log.error("调用阿里云接口创建任务失败，错误码：{}，错误信息：{}", errorCode, responseMessage);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用阿里云接口创建任务失败，错误码：" + errorCode + "，错误信息：" + responseMessage);
            }
            return taskResponse;
        } catch (Exception e) {
            log.error("调用阿里云接口创建任务异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用阿里云接口创建任务异常：" + e.getMessage());
        }
    }

    public GetOutPaintingTaskResponse getOutPaintingTaskResponse(String taskId) {
        // 1.校验参数
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        }

        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_TASK_STATUS_URL, workspaceId, taskId)).header("Authorization", "Bearer " + appKey).execute()) {
            if (!httpResponse.isOk()) {
                log.error("调用阿里云接口获取任务状态失败，状态码：{}，响应体：{}", httpResponse.getStatus(), httpResponse.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用阿里云接口获取任务状态失败，状态码：" + httpResponse.getStatus());
            }

            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        } catch (Exception e) {
            log.error("调用阿里云接口获取任务状态异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用阿里云接口获取任务状态异常：" + e.getMessage());
        }
    }
}
