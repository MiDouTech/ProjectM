package com.mido.pm.common.datascope;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.mido.pm.common.security.CurrentUser;
import com.mido.pm.common.security.DataScope;
import com.mido.pm.common.security.UserContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 可复用的数据范围拦截器（MyBatis-Plus InnerInterceptor）。
 * 仅当服务通过 {@link DataScopeContext} 显式声明、且存在登录用户时生效：
 * 取当前用户对该资源的有效数据范围 → 构造条件 → 注入查询 SQL。
 */
public class DataScopeInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        DataScopeContext.Setting setting = DataScopeContext.get();
        CurrentUser user = UserContext.get();
        if (setting == null || user == null) {
            return;
        }
        DataScope scope = user.effectiveScope(setting.resource());
        String condition = DataScopeHelper.buildCondition(user, scope, setting.deptColumn(),
                setting.userColumn(), setting.memberColumn(), setting.memberIds());
        if (condition == null) {
            // ALL：不限制
            return;
        }
        PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
        mpBs.sql(DataScopeSqlInjector.inject(mpBs.sql(), condition));
    }
}
