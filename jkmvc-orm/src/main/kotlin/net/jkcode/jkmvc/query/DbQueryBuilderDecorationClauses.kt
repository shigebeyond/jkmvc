package net.jkcode.jkmvc.query

import net.jkcode.jkutil.common.cloneProperties
import net.jkcode.jkmvc.db.IDb
import java.util.*
import kotlin.reflect.KFunction3

/**
 * sql修饰子句的模拟构建
 *     每个修饰子句(如where xxx and yyy/group by xxx, yyy)包含多个子表达式(如where可以有多个条件子表达式, 如name="shi", age=1), 每个子表达式有多个元素组成(如name/=/"shi")
 *     每个元素有对应的处理函数
 *
 * @author shijianhang
 * @date 2016-10-13
 */
abstract class DbQueryBuilderDecorationClauses<T>(protected val operator: String /* 修饰符， 如where/group by */,
                                                  protected val elementHandlers: Array<KFunction3<DbQueryBuilderDecoration, IDb, *, String>?> /* 每个元素的处理器, 可视为列的处理*/
) : IDbQueryBuilderDecorationClauses<T>, Cloneable {
    /**
     * 子表达式, 可视为行
     */
    protected val subexps: LinkedList<T> = LinkedList();

    /**
     * 编译多个子表达式
     * @param query 查询构建器
     * @param db 数据库连接
     * @param sql 保存编译的sql
     */
    public override fun compile(query: DbQueryBuilderDecoration, db: IDb, sql:StringBuilder) {
        if (subexps.isEmpty())
            return;

        // 逐个子表达式编译+合并
        sql.append(operator).append(' ');
        for(i in 0..(subexps.size - 1))
            compileSubexp(subexps[i], i, query, db, sql);
    }

    /**
     * 清空
     * @return
     */
    public override fun clear(): IDbQueryBuilderDecorationClauses<T> {
        subexps.clear();
        return this;
    }

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone()
        // 复制复杂属性: 子表达式
        o.cloneProperties("subexps")
        return o;
    }
}