package com.ziwen.moudle.config;

import com.ziwen.moudle.entity.BaseEntity;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Properties;

/**
 * MyBatis 自动填充拦截器
 * 自动填充 createTime、updateTime、isDeleted 字段
 *
 * @author ziwen
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class MybatisAutoFillInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        SqlCommandType sqlCommandType = ms.getSqlCommandType();

        if (parameter instanceof BaseEntity) {
            BaseEntity<?> entity = (BaseEntity<?>) parameter;
            LocalDateTime now = LocalDateTime.now();

            if (SqlCommandType.INSERT == sqlCommandType) {
                // 插入时填充
                if (entity.getCreateTime() == null) {
                    entity.setCreateTime(now);
                }
                if (entity.getUpdateTime() == null) {
                    entity.setUpdateTime(now);
                }
                if (entity.getIsDeleted() == null) {
                    entity.setIsDeleted(0);
                }
            } else if (SqlCommandType.UPDATE == sqlCommandType) {
                // 更新时填充
                entity.setUpdateTime(now);
            }
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可配置属性
    }
}
