package com.example.picturebackend;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
class RedisStringTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testStringRedisTemplate() {
        // 获取 StringRedisTemplate 对象
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();

        // key 和 value
        String key = "testKey";
        String value = "testValue";

        // 1. 测试存储和获取字符串
        operations.set(key, value);
        String storeValue = operations.get(key);
        log.info("Stored value: {}", storeValue);

        // 2. 测试更新字符串
        String upatedValue = "updatedValue";
        operations.set(key, upatedValue);
        storeValue = operations.get(key);
        log.info("Updated value: {}", storeValue);


        // 3. 测试删除字符串
        stringRedisTemplate.delete(key);
        storeValue = operations.get(key);
        log.info("Deleted value: {}", storeValue); // 应该为 null

        // 4. 查询所有键
        log.info("All keys in Redis: {}", stringRedisTemplate.keys("*"));
    }

}
