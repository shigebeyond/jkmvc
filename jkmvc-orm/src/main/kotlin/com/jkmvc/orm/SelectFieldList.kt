package com.jkmvc.orm

import java.util.*

/**
 * 查询字段
 *
 * @author shijianhang
 * @date 2017-10-10
 */
data class SelectFields(
        val myFields: List<String> /* 本模型的字段 */,
        val relatedFields: List<Pair<String, SelectFields?>> = emptyList() /* 多个 关系名 + 关联模型的字段列表 */
){
    companion object{

        /**
         * 解析查询字段
         *    解析出本模型的字段 + （关系名 + 关联模型的字段）
         *
         * @param fields 字段列表，其元素类型可以是 1 String 本模型字段名 2 Pair<String, List<String>> 关系名 + 关联模型的字段列表
         * @param relationPredicate 关系检查lambda，检查某字段是否是关系
         * @return
         */
        public fun parseSelectFields(fields:Iterator<Any>, relationPredicate: (Any) -> Boolean): SelectFields {
            val myFields = LinkedList<String>() // 本模型字段
            val relatedFields = LinkedList<Pair<String, SelectFields?>>() // 关系名 + 关联模型的字段
            for (field in fields){
                // 1 本模型字段
                if(!relationPredicate(field)){
                    myFields.add(field as String)
                    continue;
                }

                // 2 关联模型+字段
                if(field is Pair<*, *>){
                    val relatedName = field.first as String // 关系名
                    val relatedField = field.second as List<Any> // 关联模型的字段
                    relatedFields.add(relatedName to parseSelectFields(relatedField.iterator(), relationPredicate)) // 递归解析 关联模型的字段
                }else if(field is String){
                    relatedFields.add(field to null)
                }
            }
            return SelectFields(myFields, relatedFields)
        }

        /**
         * 解析查询字段
         *    解析出本模型的字段 + （关系名 + 关联模型的字段）
         *
         * @param fields 字段列表，其元素类型可以是 1 String 本模型字段名 2 Pair<String, List<String>> 关系名 + 关联模型的字段列表
         * @param relationPredicate 关系检查lambda，检查某字段是否是关系
         * @return
         */
        public fun parseSelectFields(fields:Array<out Any>, relationPredicate: (Any) -> Boolean): SelectFields {
            return parseSelectFields(fields.iterator(), relationPredicate)
        }
    }
}