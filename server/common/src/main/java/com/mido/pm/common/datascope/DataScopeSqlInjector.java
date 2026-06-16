package com.mido.pm.common.datascope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 把数据范围条件片段以 AND 注入到 SELECT 的 WHERE（基于 jsqlparser，纯函数，可单测）。
 * 解析失败或非简单 SELECT 时原样返回，绝不破坏原查询。
 */
public final class DataScopeSqlInjector {

    private static final Logger log = LoggerFactory.getLogger(DataScopeSqlInjector.class);

    private DataScopeSqlInjector() {
    }

    public static String inject(String sql, String conditionFragment) {
        if (sql == null || conditionFragment == null || conditionFragment.isBlank()) {
            return sql;
        }
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof PlainSelect plainSelect)) {
                return sql;
            }
            Expression condition = CCJSqlParserUtil.parseCondExpression(conditionFragment);
            Expression where = plainSelect.getWhere();
            plainSelect.setWhere(where == null ? condition : new AndExpression(where, condition));
            return plainSelect.toString();
        } catch (Exception e) {
            log.warn("数据范围 SQL 注入失败，按原 SQL 执行：{}", e.getMessage());
            return sql;
        }
    }
}
