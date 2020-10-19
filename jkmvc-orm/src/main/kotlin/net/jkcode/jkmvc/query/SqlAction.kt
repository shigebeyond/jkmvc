package net.jkcode.jkmvc.query

/**
 * sql的动作类型
 * @author shijianhang
 * @date 2016-10-10
 */
public enum class SqlAction {
    SELECT{  // 查
        override val template: DbQueryPartTemplate = DbQueryPartTemplate("SELECT <distinct> <columns> FROM <tables>")
    },
    INSERT{  // 增
        override val template: DbQueryPartTemplate = DbQueryPartTemplate("INSERT INTO <tables> (<columns>) <values>") // quoteColumn() 默认不加(), quote() 默认加()
    },
    UPDATE{  // 改
        override val template: DbQueryPartTemplate = DbQueryPartTemplate("UPDATE <tables> SET <columnValues>")
    },
    DELETE{  // 删
        override val template: DbQueryPartTemplate = DbQueryPartTemplate("DELETE <delTables> FROM <tables>")
    };

    abstract val template: DbQueryPartTemplate;
}