package com.jkmvc.orm

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
            val (sourceMeta, type, model, foreignKey) = relation as RelationMeta;
            if (type == RelationType.BELONGS_TO)
                this[foreignKey] = (value as Orm).pk; // 更新字段 super.set(foreignKey, value.pk);
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
    public override operator fun <T> get(name: String, defaultValue: Any?): T {
        // 获得关联对象
        if (ormMeta.hasRelation(name))
            return related(name, false) as T;

        return super.get(name, defaultValue);
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
            } else if (value !== null) {// 关联对象字段: 不处理null的值, 因为left join查询时, 关联对象可能没有匹配的行
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

            var result: Any?;
            if (newed) {  // 创建新对象
                result = relation.newModelInstance();
            }else{  // 根据关联关系来构建查询
                val query:OrmQueryBuilder
                if(relation.type == RelationType.BELONGS_TO) // 查主表
                    query = queryMaster(relation)
                else // 查从表
                    query = querySlave(relation)
                query.select(columns) // 查字段
                if(relation.type == RelationType.HAS_MANY){ // 查多个
                    result = query.findAll(transform = relation.recordTranformer)
                }else{ // 查一个
                    result = query.find(transform = relation.recordTranformer)
                }
            }

            data[name] = result;
        }

        return data[name];
    }

    /**
     * 查询关联表
     *
     * @param relation 关联关系
     * @return
     */
    public override fun queryRelated(relation: IRelationMeta): OrmQueryBuilder {
        return if(relation.type == RelationType.BELONGS_TO) // 查主表
                    queryMaster(relation)
                else // 查从表
                    querySlave(relation)
    }

    /**
     * 查询关联的从表
     *
     * @param relation 从表关系
     * @return
     */
    protected fun querySlave(relation: IRelationMeta): OrmQueryBuilder {
        return relation.queryBuilder().where(relation.foreignKey, this[relation.primaryProp]) as OrmQueryBuilder; // 从表.外键 = 主表.主键
    }

    /**
     * 查询关联的主表
     *
     * @param relation 主表关系
     * @return
     */
    protected fun queryMaster(relation: IRelationMeta): OrmQueryBuilder {
        return relation.queryBuilder().where(relation.ormMeta.primaryKey, this[relation.foreignProp]) as OrmQueryBuilder; // 主表.主键 = 从表.外键
    }
}
