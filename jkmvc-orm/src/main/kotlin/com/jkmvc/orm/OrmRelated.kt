package com.jkmvc.orm

import com.jkmvc.db.DbQueryBuilder

/**
 * ORM之关联对象操作
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmRelated: OrmPersistent() {
    /**
     * 设置对象字段值
     *
     * @param column 字段名
     * @param  value  字段值
     */
    public override operator fun set(column: String, value: Any?) {
        // 设置关联对象
        val relation = ormMeta.getRelation(column);
        if (relation != null) {
            data[column] = value;
            // 如果关联的是主表，则更新从表的外键
            if (relation.type == RelationType.BELONGS_TO)
                this[relation.foreignKey] = (value as Orm).pk; // 更新字段 super.set(foreignKey, value.pk);
            return;
        }

        super.set(column, value);
    }

    /**
     * 获得对象字段
     *
     * @param column 字段名
     * @param defaultValue 默认值
     * @return
     */
    public override operator fun <T> get(column: String, defaultValue: Any?): T {
        // 获得关联对象
        if (ormMeta.hasRelation(column))
            return related(column, false) as T;

        return super.get(column, defaultValue);
    }

    /**
     * 设置原始的字段值
     *    在从db中读数据时调用，来赋值给本对象属性
     *    要做字段转换：db字段名 -> 对象属性名
     *
     * @param data
     * @return
     */
    public override fun original(orgn: Map<String, Any?>): IOrm {
        for ((column, value) in orgn) {
            // 关联查询时，会设置关联表字段的列别名（列别名 = 表别名 : 列名），可以据此来设置关联对象的字段值
            if (!column.contains(":")){ // 自身字段
                val prop = ormMeta.column2Prop(column)
                data[prop] = value;
            } else if (value !== null) {// 关联对象字段: 不处理null值, 因为left join查询时, 关联对象可能没有匹配的行
                // 多层:
                val cols = column.split(":")
                // 获得最后一层的关联对象
                var obj:OrmRelated = this
                for (i in 0..cols.size - 2){
                    obj = obj.related(cols[i], true) as OrmRelated; // 创建关联对象
                }
                // 设置最底层的属性值
                val prop = ormMeta.column2Prop(cols.last())
                obj.data[prop] = value;
            }
        }

        loaded = true;
        return this;
    }

    /**
     * 获得字段值 -- 转为Map
     * @return
     */
    public override fun asMap(): Map<String, Any?> {
        // 转关联对象
        for((name, relation) in ormMeta.relations){
            val value = data[name]
            if(value != null){
                data[name] = when(value){
                    is Collection<*> -> (value as Collection<IOrm>).itemAsMap() // 有多个
                    is Orm -> value.asMap()  // 有一个
                    else -> value
                }
            }
        }

        // 转当前对象：由于关联对象联查时不处理null值, 因此关联对象会缺少null值的字段，这里要补上
        for(prop in ormMeta.props){
            if(!data.containsKey(prop))
                data[prop] = null
        }

        return data;
    }

    /**
     * 获得关联对象
     *
     * @param name 关联对象名
     * @param 是否创建新对象：在查询db后设置原始字段值data()时使用
     * @param columns 字段名数组: Array(column1, column2, alias to column3),
     * 													如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
     * @return
     */
    public override fun related(name: String, newed: Boolean, vararg columns: String): Any? {
        if (name !in data){
            // 获得关联关系
            val relation = ormMeta.getRelation(name)!!;

            var result: Any? = null;
            if (newed) {  // 创建新对象
                result = relation.newModelInstance();
            }else{  // 根据关联关系来构建查询
                val query:OrmQueryBuilder? = relation.queryRelated(this) // 自动构建查询条件
                if(query == null) // 如果查询为空，说明主/外键为空，则数据有问题，则不查询不赋值（一般出现在调试过程中）
                    return null;

                query.select(columns) // 查字段
                if (relation.type == RelationType.HAS_MANY) { // 查多个
                    result = query.findAll(transform = relation.recordTranformer)
                } else { // 查一个
                    result = query.find(transform = relation.recordTranformer)
                }
            }

            data[name] = result;
        }

        return data[name];
    }

    /**
     * 统计关联对象个数
     *    一般只用于一对多 hasMany 的关系
     *    一对一关系，你还统计个数干啥？
     *
     * @param name 关联对象名
     * @return
     */
    public override fun countRelated(name:String): Long {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 构建查询：自动构建查询条件
        val query = relation.queryRelated(this)
        return if(query == null) 0 else query.count()
    }

    /**
     * 删除关联对象
     *    一般用于删除 hasOne/hasMany 关系的从对象
     *    你敢删除 belongsTo 关系的主对象？
     *
     * @param name 关系名
     * @return
     */
    public override fun deleteRelated(name: String): Boolean {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 构建查询：自动构建查询条件
        val query = relation.queryRelated(this, false)
        return if(query == null) true else query.delete()
    }

    /**
     * 删除关系，不删除关联对象，只是将关联的外键给清空
     *     一般用于清空 hasOne/hasMany 关系的从对象的外键值
     *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能清空本对象的外键咯
     *
     * @param name 关系名
     * @param nullValue 外键的空值
     * @return
     */
    public override fun removeRelations(name:String, nullValue: Any?): Boolean {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 1 belongsTo：清空本对象的外键
        if(relation.type == RelationType.BELONGS_TO){
            this[relation.foreignProp] = nullValue
            return this.update()
        }

        // 2 hasOne/hasMany
        // 2.1 有中间表：删除中间表
        if(relation is MiddleRelationMeta)
            return DbQueryBuilder(ormMeta.db, relation.middleTable).where(relation.foreignKey, "=", this[relation.primaryProp]).delete();

        // 2.2 无中间表：清空关联对象的外键
        return relation.queryRelated(this)!!.set(relation.foreignKey, nullValue).update()
    }
}
