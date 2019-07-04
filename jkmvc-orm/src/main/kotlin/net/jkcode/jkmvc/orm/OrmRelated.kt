package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.Row

/**
 * ORM之关联对象操作
 *
 * @author shijianhang
 * @date 2016-10-10 上午12:52:34
 *
 */
abstract class OrmRelated : OrmPersistent() {

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
            // 设置关联对象
            data[column] = value;
            // 如果关联的是主表，则更新从表的外键
            if (value != null && relation.type == RelationType.BELONGS_TO)
                sets(relation.foreignProp, (value as Orm).pk); // 更新字段
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
    public override operator fun <T> get(column: String, defaultValue: T?): T {
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
     */
    public override fun setOriginal(orgn: Row): Unit {
        if(orgn.isEmpty())
            return

        // 设置属性值
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
                    obj = obj.related(cols[i], true) as Orm; // 创建关联对象
                }
                // 设置最底层的属性值
                val prop = ormMeta.column2Prop(cols.last())
                obj.data[prop] = value;
            }
        }

        // 标记已加载
        loaded = true;
        // 只标记一层，防止递归死循环
        for((name, relation) in ormMeta.relations){
            if(name in data)
                (data[name] as Orm).loaded = true
        }
    }

    /**
     * 获得字段值 -- 转为Map
     * @param to
     * @param expected 要设置的字段名的列表
     * @return
     */
    public override fun toMap(to: MutableMap<String, Any?>, expected: List<String>): MutableMap<String, Any?> {
        // 1 转关联对象
        for((name, relation) in ormMeta.relations){
            val value = data[name]
            if(value != null){
                to[name] = when(value){
                    is Collection<*> -> (value as Collection<IOrm>).itemToMap() // 有多个
                    is IOrm -> value.toMap()  // 有一个
                    else -> value
                }
            }
        }

        // 2 转当前对象：由于关联对象联查时不处理null值, 因此关联对象会缺少null值的字段，这里要补上
        for(prop in ormMeta.props)
            to[prop] = data[prop]

        return to;
    }


    /**
     * 从map中设置字段值
     *    对于关联对象字段值的设置: 只考虑一对一的关联对象, 不考虑一对多的关联对象
     *
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param expected 要设置的字段名的列表
     */
    public override fun fromMap(from: Map<String, Any?>, expected: List<String>): Unit {
        val columns = if (expected.isEmpty())
                            ormMeta.defaultExpectedProps
                        else
                            expected

        for(column in columns){
            val value = from[column]
            var realValue: Any? = value // Any? / Orm / List / Map
            if(value is Map<*, *>){ // 如果是map，则为关联对象
                realValue = related(column, true) // 创建关联对象
                (realValue as Orm).fromMap(value as Map<String, Any?>) // 递归设置关联对象的字段值
            }else
                set(column, value)
        }
    }

    /**
     * 从其他实体对象中设置字段值
     *    对于关联对象字段值的设置: 只考虑一对一的关联对象, 不考虑一对多的关联对象
     *
     * @param from
     */
    public override fun from(from: IOrmEntity): Unit{
        for(column in ormMeta.defaultExpectedProps) {
            val value:Any? = from[column]
            var realValue: Any? = value // Any? / Orm / List / Map
            if(value is IOrmEntity){ // 如果是IOrmEntity，则为关联对象
                realValue = related(column, true) // 创建关联对象
                (realValue as Orm).from(value) // 递归设置关联对象的字段值
            }else
                set(column, value)
        }
    }

    /**
     * 获得关联对象
     *
     * @param name 关联对象名
     * @param 是否创建新对象：在查询db后设置原始字段值data()时使用
     * @param columns 字段名数组: Array(column1, column2, alias to column3),
     * 				如 Array("name", "age", "birt" to "birthday"), 其中 name 与 age 字段不带别名, 而 birthday 字段带别名 birt
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

                query.select(*columns) // 查字段
                if (relation.type == RelationType.HAS_MANY) { // 查多个
                    result = query.findAll(transform = relation.rowTransformer)
                } else { // 查一个
                    result = query.find(transform = relation.rowTransformer)
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
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    public override fun countRelation(name:String, fkInMany: Any?): Int {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 构建查询：自动构建查询条件
        val query = relation.queryRelated(this, fkInMany)
        return if(query == null) 0 else query.count()
    }

    /**
     * 删除关联对象
     *    一般用于删除 hasOne/hasMany 关系的从对象
     *    你敢删除 belongsTo 关系的主对象？
     *
     * @param name 关系名
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    public override fun deleteRelated(name: String, fkInMany: Any?): Boolean {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;

        // 1 有中间表的关联对象
        if(relation is MiddleRelationMeta)
            return deleteMiddleRelated(relation, fkInMany)

        // 2 普通关联对象
        // 构建查询：自动构建查询条件
        val query = relation.queryRelated(this, false)
        return if(query == null) true else query.delete()
    }

    /**
     * 删除有中间表的关联对象
     *
     * @param relation
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    protected fun deleteMiddleRelated(relation: MiddleRelationMeta, fkInMany: Any?): Boolean {
        val db = ormMeta.db
        return db.transaction {
            // 子查询
            val subquery = relation.queryMiddleTable(this, fkInMany)
            if(subquery != null){
                // 删除关联对象
                relation.ormMeta.queryBuilder().where(relation.farPrimaryKey, "IN", subquery.select(*relation.farForeignKey.columns)).delete()
                // 删除中间表
                subquery.delete()
            }
            true
        }
    }

    /**
     * 添加关系（添加关联的外键值）
     *     一般用于添加 hasOne/hasMany 关系的从对象的外键值
     *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能添加本对象的外键咯
     *
     * @param name 关系名
     * @param value 外键值Any | 关联对象IOrm
     * @return
     */
    public override fun addRelation(name:String, value: Any): Boolean {
        //更新外键
        return updateForeighKey(name, value){ relation: MiddleRelationMeta, fkInMany: Any? ->
            // 插入中间表
            relation.insertMiddleTable(this, value) > 0
        }
    }

    /**
     * 删除关系，不删除关联对象，只是将关联的外键给清空
     *     一般用于清空 hasOne/hasMany 关系的从对象的外键值
     *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能清空本对象的外键咯
     *
     * @param name 关系名
     * @param nullValue 外键的空值
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    public override fun removeRelations(name:String, nullValue: Any?, fkInMany: Any?): Boolean {
        //更新外键
        return updateForeighKey(name, nullValue, fkInMany){ relation: MiddleRelationMeta, fkInMany: Any? ->
            // 删除中间表
            val query = relation.queryMiddleTable(this, fkInMany)
            if(query == null)
                true
            else
                query.delete()
        }
    }

    /**
     * 更新关系外键
     *
     * @param name 关系名
     * @param value 外键值Any | 关联对象IOrm
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则更新所有关系, 否则更新单个关系
     * @param middleForeighKeyUpdater 有中间表的关联关系的外键更新函数
     * @return
     */
    protected fun updateForeighKey(name:String, value: Any?, fkInMany: Any? = null, middleForeighKeyUpdater: ((relation: MiddleRelationMeta, fkInMany: Any?) -> Boolean)): Boolean {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 1 belongsTo：更新本对象的外键
        if(relation.type == RelationType.BELONGS_TO){
            val value2 = if (value is IOrm) value.gets(relation.primaryProp) else value
            this.sets(relation.foreignProp, value2)
            return this.update()
        }

        // 2 hasOne/hasMany
        // 2.1 有中间表：
        if(relation is MiddleRelationMeta)
            return middleForeighKeyUpdater(relation, fkInMany)

        // 2.2 无中间表：更新关联对象的外键
        if(value is IOrm){ // 2.2.1 新加值
            //value[relation.foreignProp] = this[relation.primaryProp]
            value.sets(relation.foreignProp, this.gets(relation.primaryProp))
            return value.update()
        }
        // 2.2.2 改旧值
        return relation.queryRelated(this, fkInMany)!!.set(relation.foreignKey, value).update()
    }
}
