package com.example.picturebackend.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.example.picturebackend.exception.BusinessException;
import com.example.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlPageApi {

    /**
     * 获取以图搜图页面地址
     *
     * @param imageUrl 图片地址
     *
     * @return 页面地址
     */
    public static String getImagePageUrl(String imageUrl) {
        //查询字符串
        // uptime=1789561975516
        // 表单数据
        //image https://p3.itc.cn/q_70/images01/20221222/95583d4c535a4a5eba3aabfddd8e3c3f.jpeg
        //tn pc
        //from pc
        //image_source PC_UPLOAD_URL
        //sdkParams

        // 1. 构建请求URL
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        String acsToken = "1789535708934_1789567845554_mY0043QDgGIUTKFFVodlaGYuYGkXxWUOh77r5NjoOJADcqjee+EEIDz5Xv7JQYKs+PzHmmp0beLqgA0VT/9koWIMJ9yIA2mestJXA4au35kAcA+uMkRFXvAVZfZ7bYnGwls8S3j3bKi++GKDXKxWwkQcAVECpUcj2qXmKqaYYpboVqCVxGOrTR+Bn3Xdr75s5E5lfwqj2MMZU92M3ciVy255Vg2TZ8q5IiZgCnZqILrRBoLllKgWIyEfjXwBnrtd408EIhkwfq311dgwNlkGEY91S7fBLJGNkZ2R6RACLmeVRODXYsCDhXVKy+7bsaPwjMzw1CyW2/cGbrjpx++Z8aFTCco6hLbrsB/S1nIHtut63DwNXm9jEeGnkFGWCaiSpRV49cNiw/NpBR9jO0EgbvELdbNDbaTqrqom+y+k8MA=";
        // 2. 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 3. 构建请求URL
        String requestUrl = "https://graph.baidu.com/upload?uptime=" + uptime;
        // 4. 使用Hutool发送POST请求
        try {
            HttpResponse response = HttpRequest.post(requestUrl)
                    .form(formData)
                    .header("Acs-Token", acsToken)
                    .timeout(10000)
                    .execute();

            // 5. 获取响应体, 判断响应状态码
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败，状态码：" + response.getStatus());
            }

            // 6. 响应成功，获取响应体中的JSON数据
            String responseBody = response.body();
            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);
            // 7. 判断响应体中的status字段是否为0，若不是则抛出异常
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败，result：" + result);
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 8. 对rawUrl进行解码，获取最终的页面地址
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 9.如果searchResultUrl为空，则抛出异常
            if (searchResultUrl == null || searchResultUrl.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败，未返回有效页面地址：" + searchResultUrl);
            }

            return searchResultUrl;

        } catch (Exception e) {
            log.error("Error occurred while getting image page URL", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败，发生异常：" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String imageUrl = "https://p3.itc.cn/q_70/images01/20221222/95583d4c535a4a5eba3aabfddd8e3c3f.jpeg";
        String pageUrl = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，页面地址: " + pageUrl);
    }
}
