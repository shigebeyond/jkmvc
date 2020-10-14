package net.jkcode.jkmvc.orm

import java.util.*

/**
 * 关系名 + 关联模型的字段列表
 */
data class RelatedSelectColumnList(
        public val name: CharSequence, // 关系名, 类型 String|DbExpr(关系名+别名)
        public val columns: SelectColumnList? = null // 查询字段
)

/**
 * 默认的查询字段
 */
val defaultSelectColumnList = SelectColumnList(emptyList())

/**
 * 查询字段
 *
 * @author shijianhang
 * @date 2017-10-10
 */
data class SelectColumnList(
        public val myColumns: List<String>, // 本模型的字段
        public val relatedColumns: List<RelatedSelectColumnList> = emptyList() // 多个 关系名 + 关联模型的字段列表
){
    companion object{

        /**
         * 解析查询字段
         *    解析出本模型的字段 + （关系名 + 关联模型的字段）
         *
         * @param sourceMeta 源模型元数据
         * @param columns 字段列表，其元素类型可以是 1 String 本模型字段名 2 RelatedSelectColumnList 关系名 + 关联模型的字段列表
         *               如listOf("id", "name","org", "dept" to listOf("id", "title"), DbExpr("group", "group2") to listOf("*")), 其中本模型要显示id与name字段，org是关联模型名, 要显示所有字段, dept是关联模型名，要显示id与title字段, group是关联模型名, group2是别名
         * @return
         */
        public fun parse(sourceMeta: IOrmMeta, columns:Iterator<Any>): SelectColumnList {
            val myColoumns = LinkedList<String>() // 本模型字段
            val relatedColumns = LinkedList<RelatedSelectColumnList>() // 关系名 + 关联模型的字段
            for (col in columns){
                // 获得关系名
                var subname: CharSequence // 关系名, 类型 String|DbExpr(关系名+别名)
                val subcolumns: List<Any>? // 关联模型的字段列表
                when(col){
                    is Pair<*, *> -> {
                        subname = col.first as CharSequence
                        subcolumns = col.second as List<Any>
                    }
                    is String -> {
                        subname = col
                        subcolumns = null
                    }
                    else ->
                        //throw IllegalArgumentException("查询字段参数类型必须是：1 String 本模型字段名 2 RelatedSelectColumnList 关系名 + 关联模型的字段列表")
                        throw IllegalArgumentException("Select column's class only accept：1 `String` then represent this model's field 2 `RelatedSelectColumnList` then represent relation name and related model's fields")
                }

                // 检查关系
                val relation = sourceMeta.getRelation(subname.toString()) // 兼容 name 类型是 DbExpr, 用 DbExpr.toString() 来引用关系名

                // 处理本模型字段
                if(relation == null){
                    myColoumns.add(col as String)
                    continue;
                }

                // 递归解析 关联模型的字段
                val selects = if(subcolumns == null)
                                null
                            else
                                parse(relation.ormMeta, subcolumns) // 递归解析 关联模型的字段
                relatedColumns.add(RelatedSelectColumnList(subname, selects))
            }
            return SelectColumnList(myColoumns, relatedColumns)
        }

        /**
         * 解析查询字段
         *    解析出本模型的字段 + （关系名 + 关联模型的字段）
         *
         * @param sourceMeta 源模型元数据
         * @param fields 字段列表，其元素类型可以是 1 String 本模型字段名 2 RelatedSelectColumnList 关系名 + 关联模型的字段列表
         *               如arrayOf("id", "name","org", "dept" to listOf("id", "title"), DbExpr("group", "group2") to listOf("*")), 其中本模型要显示id与name字段，org是关联模型名, 要显示所有字段, dept是关联模型名，要显示id与title字段, group是关联模型名, group2是别名
         * @return
         */
        public fun parse(sourceMeta: IOrmMeta, fields:Array<out Any>): SelectColumnList {
            if(fields.isEmpty())
                return defaultSelectColumnList

            return parse(sourceMeta, fields.iterator())
        }

        /**
         * 解析查询字段
         *    解析出本模型的字段 + （关系名 + 关联模型的字段）
         *
         * @param sourceMeta 源模型元数据
         * @param fields 字段列表，其元素类型可以是 1 String 本模型字段名 2 RelatedSelectColumnList 关系名 + 关联模型的字段列表
         *               如listOf("id", "name","org", "dept" to listOf("id", "title"), DbExpr("group", "group2") to listOf("*")), 其中本模型要显示id与name字段，org是关联模型名, 要显示所有字段, dept是关联模型名，要显示id与title字段, group是关联模型名, group2是别名
         * @return
         */
        public fun parse(sourceMeta: IOrmMeta, fields:List<Any>): SelectColumnList {
            if(fields.isEmpty())
                return defaultSelectColumnList

            return parse(sourceMeta, fields.iterator())
        }
    }

    /**
     * 遍历本模型的字段
     *
     * @param action 处理字段的lambda，只接收一个参数: 1 字段名
     */
    public fun forEachMyColumns(action: (column: String) -> Unit) {
        myColumns.forEach(action)
    }

    /**
     * 遍历关联模型的字段
     *
     * @param action 处理字段的lambda，接收两个参数: 1 关系名 2 关联模型的字段列表
     */
    public fun forEachRelatedColumns(action: (name: CharSequence, columns: SelectColumnList?) -> Unit) {
        for(field in relatedColumns){
            action(field.name, field.columns)
        }
    }
}