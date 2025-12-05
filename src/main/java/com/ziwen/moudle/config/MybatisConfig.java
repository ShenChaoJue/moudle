package com.ziwen.moudle.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 *
 * @author ziwen
 */
@Configuration
@MapperScan("com.ziwen.moudle.mapper")
public class MybatisConfig {

}
