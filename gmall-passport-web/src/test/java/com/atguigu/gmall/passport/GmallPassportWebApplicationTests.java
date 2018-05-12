package com.atguigu.gmall.passport;

import com.atguigu.gmall.passport.util.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

	@Test
	public void contextLoads() {

		String key = "atguigu";
		String ip = "192.168.242.128";

		Map map = new HashMap();
		map.put("userId", "123");
		map.put("nickName", "Chen");

		String token = JwtUtil.encode(key, map, ip);

		System.out.println("token = " + token);

	}

}
