package com.atguigu.gmall.util;

import org.apache.catalina.Host;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

    JedisPool jedisPool;

    int timeout = 5 * 1000;

    public void initPool(String host, int port){

        Jedis jedis = new Jedis("192.168.242.128", 6379);
        jedis.set("k1", "v10");
        jedis.close();


        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

        jedisPoolConfig.setMaxTotal(200);
        jedisPoolConfig.setMaxIdle(20);
        jedisPoolConfig.setMinIdle(10);
        jedisPoolConfig.setBlockWhenExhausted(true);
        jedisPoolConfig.setMaxWaitMillis(2000);
        jedisPoolConfig.setTestOnBorrow(true);


        jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout);
    }


    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();

        return jedis;
    }




}
