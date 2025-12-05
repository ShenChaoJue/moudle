package com.ziwen.moudle.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis 配置类
 *
 * @author ziwen
 */
@Configuration
@MapperScan("com.ziwen.moudle.mapper")
public class MybatisConfig {

    @Autowired(required = false)
    private MybatisAutoFillInterceptor mybatisAutoFillInterceptor;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);

        // 设置映射文件位置
        sqlSessionFactoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml")
        );

        // 设置配置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);

        // 添加自动填充拦截器
        if (mybatisAutoFillInterceptor != null) {
            configuration.addInterceptor(mybatisAutoFillInterceptor);
        }

        sqlSessionFactoryBean.setConfiguration(configuration);

        return sqlSessionFactoryBean.getObject();
    }
}
