package com.atguigu.gmall.config;


import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:disabled}")
    String redisHost;

    @Value("${spring.redis.port:0}")
    String redisPort;





    @Bean
    public RedisUtil getRedisUtil(){

        if(redisHost.equals("disabled")){
            return null;
        }

        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initPool(redisHost, Integer.parseInt(redisPort));

        return redisUtil;
    }


}
