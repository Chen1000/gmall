package com.atguigu.gmall.usermanage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.atguigu.gmall.usermanage.mapper")
public class GamallUsermangeApplication {

	public static void main(String[] args) {

		SpringApplication.run(GamallUsermangeApplication.class, args);
	}
}
