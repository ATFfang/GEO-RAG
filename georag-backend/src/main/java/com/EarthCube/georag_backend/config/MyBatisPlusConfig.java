package com.EarthCube.georag_backend.config;

import com.EarthCube.georag_backend.handler.MyMetaObjectHandler;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
public class MyBatisPlusConfig {
    @Autowired
    private MyMetaObjectHandler myMetaObjectHandler;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // 注意：这里必须用 MybatisSqlSessionFactoryBean 而不是原生的 SqlSessionFactoryBean
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 如果你有 XML 映射文件，取消下面这行的注释
        // factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(myMetaObjectHandler); // 挂载处理类
        factoryBean.setGlobalConfig(globalConfig);

        // 如果你有分页插件，也需要手动设置进去
//        factoryBean.setPlugins(mybatisPlusInterceptor());
        return factoryBean.getObject();
    }
}