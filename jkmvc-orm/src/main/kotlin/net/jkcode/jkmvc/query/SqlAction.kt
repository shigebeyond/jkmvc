package net.jkcode.jkmvc.query

/**
 * sql的动作类型
 * @author shijianhang
 * @date 2016-10-10
 */
public enum class SqlAction {
    SELECT{  // 查
        override val template: DbQueryPartTemplte = DbQueryPartTemplte("SELECT <distinct> <columns> FROM <table>")
    },
    INSERT{  // 增
        override val template: DbQueryPartTemplte = DbQueryPartTemplte("INSERT INTO <table> (<columns>) <values>") // quoteColumn() 默认不加(), quote() 默认加()
    },
    UPDATE{  // 改
        override val template: DbQueryPartTemplte = DbQueryPartTemplte("UPDATE <table> SET <columnValues>")
    },
    DELETE{  // 删
        override val template: DbQueryPartTemplte = DbQueryPartTemplte("DELETE FROM <table>")
    };

    abstract val template: DbQueryPartTemplte;
}