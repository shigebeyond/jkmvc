package net.jkcode.jkmvc.db

import net.jkcode.jkmvc.query.DbExpr

/**
 * Db标识符(表名/字段名)转义器
 *
 * @ClassName: IDbIdentifierQuoter
 * @Description:
 * @author shijianhang<772910474@qq.com>
 * @date 2018-11-21 7:28 PM
 */
interface IDbIdentifierQuoter{

    /**
     * 转义表名
     *   mysql为`table`
     *   oracle为"table"
     *   sql server为"table" [table]
     *
     * @param table 表名或别名 DbExpr
     * @return
     */
    fun quoteTable(table:CharSequence):String {
        return if(table is DbExpr)
                    table.quoteIdentifier(this)
                else
                    quoteIdentifier(table.toString())
    }

    /**
     * 转义表别名
     *   mysql为`table`
     *   oracle为"table"
     *   sql server为"table" [table]
     *
     * @param table 表名或别名 DbExpr
     * @return
     */
    fun quoteTableAlias(table:CharSequence):String {
        return if(table is DbExpr)
                    table.quoteAlias(this)
                else
                    quoteIdentifier(table.toString())
    }

    /**
     * 转义字段名
     *   mysql为`column`
     *   oracle为"column"
     *   sql server为"column" [column]
     *
     * @param column 字段名, 可能是别名 DbExpr
     * @return
     */
    fun quoteColumn(column:CharSequence):String {
        var table = "";
        var col: String; // 字段
        var alias:String? = null; // 别名
        var colQuoting = true // 是否转义字段
        if(column is DbExpr){
            col = column.exp.toString()
            alias = column.alias
            colQuoting = column.expQuoting
        }else{
            col = column.toString()
            // 空格分割别名, 如 "sum(a) num"
            val iSpace = column.lastIndexOf(' '); // 空格位置
            if(iSpace > -1){
                alias = col.substring(iSpace + 1)
                col = col.substring(0, iSpace)
            }
            // 非函数表达式才转义
            colQuoting = "^\\w[\\w\\d_\\.\\*]*".toRegex().matches(col)
        }

        // 转义字段
        if (colQuoting) {
            // 表名
            if(col.contains('.')){
                var arr = col.split('.');
                table = "${quoteIdentifier(arr[0])}.";
                col = arr[1]
            }

            // 字段名
            if(col == "*" || isKeyword(col)) { // * 或 关键字不转义
                //...
            }else{ // 其他字段转义
                col = quoteIdentifier(col)
            }
        }

        // 字段别名
        if(alias == null || col == alias)
            return if(table == "") col else "$table$col";

        return "$table$col AS ${quoteIdentifier(alias)}"; // 转义
    }

    /**
     * 是否关键字
     * @param col 列
     * @return
     */
    fun isKeyword(col: String): Boolean

    /**
     * 转义标识符(表名/字段名)
     * @param 表名或字段名
     * @return
     */
    fun quoteIdentifier(id: String): String
}