package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.DbResultRow

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
            _data[column] = value;
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
     * 设置原始的多个字段值
     *    在从db中读数据时调用，来赋值给本对象属性
     *    要做字段名转换：db字段名 -> 对象属性名
     *    要做字段值转换: 反序列化
     *
     * @param data
     */
    public override fun setOriginal(orgn: DbResultRow) {
        // 设置属性值
        orgn.forEach { (column, value) ->
            // 关联查询时，会设置关联表字段的列别名（列别名 = 表别名 : 列名），可以据此来设置关联对象的字段值
            if (!column.contains(":")){ // 自身字段
                setOriginal(column, value)
            } else if (value !== null) {// 关联对象字段: 不处理null值, 因为left join查询时, 关联对象可能没有匹配的行
                // 多层:
                val cols = column.split(":")
                // 获得最后一层的关联对象
                var obj:OrmRelated = this
                for (i in 0..cols.size - 2){
                    obj = obj.related(cols[i], true) as Orm; // 创建关联对象
                }
                // 设置最底层的属性值
                obj.setOriginal(cols.last(), value)
            }
        }

        // 标记已加载
        loaded = true;
        // 只标记一层，防止递归死循环
        for((name, relation) in ormMeta.relations){
            val value = _data[name]
            if(value != null)
                (value as Orm).loaded = true
        }
    }

    /**
     * 从map中设置字段值
     *    对于关联对象字段值的设置: 只考虑一对一的关联对象, 不考虑一对多的关联对象
     *
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     */
    public override fun fromMap(from: Map<String, Any?>, include: List<String>, exclude: List<String>) {
        val columns = if (include.isEmpty())
                            ormMeta.propsAndRelations
                        else
                            include

        for(column in columns){
            if(exclude.contains(column))
                continue

            val value = from[column]
            if(value is Map<*, *>){ // 如果是map，则为关联对象
                val realValue = related(column, true) // 创建关联对象
                (realValue as Orm).fromMap(value as Map<String, Any?>) // 递归设置关联对象的字段值
            }else
                set(column, value)
        }
    }

    /**
     * 获得字段值 -- 转为Map
     * @param to
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     * @return
     */
    public override fun toMap(to: MutableMap<String, Any?>, include: List<String>, exclude: List<String>): MutableMap<String, Any?> {
        val columns = if (include.isEmpty())
            ormMeta.props // 只补全到当前对象属性, 不包含关联对象(后面单独处理): 由于关联对象联查时不处理null值, 因此关联对象会缺少null值的字段，这里要补上
        else
            include

        // 1 转当前对象
        super.toMap(to, include, exclude)

        // 2 转关联对象
        for((name, relation) in ormMeta.relations){
            val need = (include.isEmpty() || include.contains(name)) && !exclude.contains(name)
            if(!need)
                continue

            val value = _data[name]
            if(value != null){ // 有才输出
                to[name] = when(value){
                    is Collection<*> -> (value as Collection<IOrm>).toMaps() // 有多个
                    is IOrm -> value.toMap()  // 有一个
                    else -> value
                }
            }
        }

        return to;
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
        if (name !in _data){
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
                    result = query.findRows(transform = relation.modelRowTransformer)
                } else { // 查一个
                    result = query.findRow(transform = relation.modelRowTransformer)
                }
            }

            _data[name] = result;
        }

        return _data[name];
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

        // 不能删除 belongsTo 关联对象
        if(relation.type == RelationType.BELONGS_TO)
            throw OrmException("不能删除模型[${ormMeta.name}]的 belongsTo 关联对象[$name]");

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
        return updateForeighKey(name, value){ relation: IRelationMeta -> // hasOne/hasMany的关联关系的外键手动更新
            if(relation is MiddleRelationMeta) { // 有中间表: 插入中间表记录
                relation.insertMiddleTable(this, value) > 0
            }else{ // 无中间表: 更新从表外键
                relation.queryBuilder().where(relation.ormMeta.primaryKey, value).set(relation.foreignKey, this.pk).update()
            }
        }
    }

    /**
     * 删除关系，不删除关联对象，只是将关联的外键给清空
     *     一般用于清空 hasOne/hasMany 关系的从对象的外键值
     *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能清空本对象的外键咯
     *
     * @param name 关系名
     * @param nullValue 外键的空值, 标识删除关系, 默认null
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    public override fun removeRelations(name:String, nullValue: Any?, fkInMany: Any?): Boolean {
        //更新外键
        return updateForeighKey(name, nullValue, fkInMany){ relation: IRelationMeta -> // hasOne/hasMany的关联关系的外键手动更新
            if(relation is MiddleRelationMeta) { // 有中间表: 删除中间表记录
                val query = relation.queryMiddleTable(this, fkInMany)
                if (query == null)
                    true
                else
                    query.delete()
            }else{ // 无中间表: 改旧值
                relation.queryRelated(this, fkInMany)!!.set(relation.foreignKey, nullValue).update()
            }
        }
    }

    /**
     * 更新关系外键
     *
     * @param name 关系名
     * @param value 外键值Any | 关联对象IOrm
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则更新所有关系, 否则更新单个关系
     * @param hasNRelationForeighKeyUpdater hasOne/hasMany的关联关系的外键更新函数
     * @return
     */
    protected fun updateForeighKey(name:String, value: Any?, fkInMany: Any? = null, hasNRelationForeighKeyUpdater: ((relation: IRelationMeta) -> Boolean)): Boolean {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 1 belongsTo：更新本对象的外键
        if(relation.type == RelationType.BELONGS_TO){
            val value2 = if (value is IOrm) value.gets(relation.primaryProp) else value
            this.sets(relation.foreignProp, value2)
            return this.update()
        }

        // 2 hasOne/hasMany
        // 2.1 有中间表： 手动更新
        if(relation is MiddleRelationMeta)
            return hasNRelationForeighKeyUpdater(relation)

        // 2.2 无中间表：更新关联对象的外键
        // 2.2.1 orm对象自动更新
        if(value is IOrm){
            //value[relation.foreignProp] = this[relation.primaryProp]
            value.sets(relation.foreignProp, this.gets(relation.primaryProp))
            return value.update()
        }
        // 2.2.2 手动更新 改旧值
        return hasNRelationForeighKeyUpdater(relation)
    }

    /**
     * 添加 _data 中的 hasOne/hasMany 的关联关系
     *   仅用在 create/update() 方法中
     *   针对 _data 中改过的关联关系
     *   for jkerp
     */
    internal override fun addHasNRelations(){
        for((name, relation) in ormMeta.relations){
            // 仅处理 hasOne/hasMany 的关联关系
            if(relation.type == RelationType.BELONGS_TO)
                continue

            val value = _data[name]
            if(value != null)
                addRelation(name, value) // 添加关系
        }
    }

    /**
     * 删除 hasOne/hasMany 的关联关系
     *   仅用在 update()/delete() 方法中
     *   for jkerp
     *
     * @param byDelete 是否delete()调用, 否则update()调用
     */
    internal override fun removeHasNRelations(byDelete: Boolean) {
        for((name, relation) in ormMeta.relations){
            // 仅处理 hasOne/hasMany 的关联关系
            if(relation.type == RelationType.BELONGS_TO)
                continue

            // 1 删除关联对象(包含关系)
            if(byDelete && relation.cascadeDeleted) {
                //deleteRelated(name) // 无法触发删除的前置后置回调
                // 为了能触发删除的前置后置回调，　因此使用 Orm.delete()　实现
                // 查询关联对象
                val related = related(name)
                // 逐个递归删除
                when(related){
                    is Collection<*> -> (related as Collection<IOrm>).forEach{ it.delete(true) }
                    is IOrm -> related.delete(true)
                }
                continue
            }

            // 2 仅删除关联关系
            removeRelations(name)
        }
    }

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *
     * @param name 关系名
     * @param fkInMany hasMany关系下的单个外键值，如果为null，则更新所有关系, 否则更新单个关系
     * @param withTableAlias 是否带表前缀
     * @return
     */
    fun queryRelated(name: String, fkInMany: Any? = null, withTableAlias:Boolean = true): OrmQueryBuilder?{
        // 获得关联关系
        val relation = ormMeta.getRelation(name)
        return relation?.queryRelated(this, fkInMany, withTableAlias)
    }
}
