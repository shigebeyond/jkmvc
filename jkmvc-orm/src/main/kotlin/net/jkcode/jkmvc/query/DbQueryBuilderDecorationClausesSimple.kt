package net.jkcode.jkmvc.query

import net.jkcode.jkmvc.db.IDb
import kotlin.math.min
import kotlin.reflect.KFunction2

/**
 * 单词+连接符
 */
typealias WordsAndDelimiter = Pair<Array<Any?>, String>

/**
 * 简单的(sql修饰)子句
 *
 * 	在子表达式的拼接中，何时拼接连接符？
 *     1 在compile()时拼接连接符 => 你需要单独保存每个子表达式对应的连接符，在拼接时取出
 *     2 在addSubexp()就将连接符也记录到子表达式中 => 在compile()时直接连接子表达式的内容就行，不需要关心连接符的特殊处理
 *     我采用的是第二种
 *
 * @author shijianhang
 * @date 2016-10-13
 */
class DbQueryBuilderDecorationClausesSimple(operator: String /* 修饰符， 如where/group by */,
                                            elementHandlers: Array<KFunction2 <IDb, *, String>?> /* 每个元素的处理器, 可视为列的处理*/,
                                            protected val afterGroup:Boolean = false /* 跟在分组 DbQueryBuilderDecorationClausesGroup 后面 */
) : DbQueryBuilderDecorationClauses<WordsAndDelimiter>/* subexps 是单词+连接符(针对where子句，放子表达式前面) */(operator, elementHandlers) {
    /**
     * 添加一个子表达式+连接符
     *
     * @param subexp 子表达式
     * @param delimiter 当前子表达式的连接符
     * @return
     */
    public override fun addSubexp(subexp: Array<Any?>, delimiter: String): IDbQueryBuilderDecorationClauses<WordsAndDelimiter> {
        // 将连接符也记录到子表达式中, 忽略第一个子表达式的连接符 => 编译好子表达式直接拼接就行
        subexps.add(Pair(subexp, delimiter));
        return this;
    }

    /**
     * 编译一个子表达式
     *
     * @param subexp 子表达式
     * @param j 索引
     * @param db 数据库连接
     * @param sql 保存编译的sql
     */
    public override fun compileSubexp(subexp: WordsAndDelimiter, j:Int, db: IDb, sql: StringBuilder): Unit {
        val (exp, delimiter) = subexp;

        // 针对where子句，要在前面插入连接符
        // 一般第一个元素不加，但跟在分组后面的第一个元素要加
        if(j != 0 || afterGroup)
            sql.append(delimiter).append(' ');

        // 遍历处理器来处理对应元素(单词)
        val size = min(elementHandlers.size, exp.size)
        for (i in 0 until size) {
            val handler: KFunction2 <IDb, *, String>? = elementHandlers[i];
            // 处理某个元素(单词)的值
            var value: Any? = exp[i];
            if (handler != null) {
                value = handler.call(db, value); // 调用元素处理函数
            }
            sql.append(value).append(' '); // 用空格拼接多个元素
        }
    }

    /**
     * 开启一个分组
     *
     * @param delimiter 连接符
     * @return
     */
    public override fun open(delimiter: String): IDbQueryBuilderDecorationClauses<WordsAndDelimiter> {
        throw UnsupportedOperationException("not implemented")
    }

    /**
     * 结束一个分组
     *
     * @return
     */
    public override fun close(): IDbQueryBuilderDecorationClauses<WordsAndDelimiter> {
        throw UnsupportedOperationException("not implemented")
    }
}