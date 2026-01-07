package com.EarthCube.georag_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.EarthCube.georag_backend.mapper")
public class GeoRagApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeoRagApplication.class, args);
	}

}
