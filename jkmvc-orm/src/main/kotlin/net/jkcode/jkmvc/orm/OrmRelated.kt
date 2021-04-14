package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.DbResultRow
import net.jkcode.jkmvc.orm.relation.HasNThroughRelation
import net.jkcode.jkmvc.orm.serialize.toMaps
import net.jkcode.jkmvc.query.DbExpr
import java.util.*
import kotlin.collections.HashSet

/**
 * ORM之关联对象操作
 *
 * 关于关联删除:
 *      1. deleteRelated() -- 删除关系 + 对象, 会级联删除
 *      2. removeRelations() -- 仅删除关系, 不删除对象, 不会级联删除
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
            if (value != null && relation.isBelongsTo)
                sets(relation.foreignProp, (value as Orm).pk); // 更新字段
            return;
        }

        super.set(column, value);
    }

    /**
     * 获得对象字段
     *
     * @param column 字段名
     * @return
     */
    public override operator fun <T> get(column: String): T {
        // 获得关联对象
        if (ormMeta.hasRelation(column))
            return getRelatedOrQuery(column) as T;

        // 获得回调的关联对象
        if (ormMeta.hasCbRelation(column))
            return cbRelated(column) as T;

        return super.get(column);
    }

    /**
     * 获得或设置列表类型的属性值
     *   仅用于设置一对多的属性值
     *   不能使用 get(key) 来获得原来属性值, 因为他会延迟加载关联对象
     *
     * @param key
     * @return
     */
    internal fun getOrPutList(key: String): MutableList<Any?> {
        // 获得属性值: 不能使用 get(key) 来获得原来属性值, 因为他会延迟加载关联对象
        var value = _data[key] as MutableList<Any?>?
        if (value == null) {
            // 设置属性值
            value = LinkedList()
            _data[key] = value
        }
        return value
    }

    /**
     * 设置原始的一行多个字段值
     *    在从db中读数据时调用，来赋值单行给本对象属性
     *    要做字段名转换：db字段名 -> 对象属性名
     *    要做字段值转换: 反序列化
     *
     * @param data
     */
    public override fun setOriginal(orgn: DbResultRow) {
        // 遍历并设置每个属性值
        val mayNullRelatedNames = HashSet<String>() // 可能为null的多层关联对象名
        orgn.forEach { (column, value) ->
            // 关联查询时，会设置关联表字段的列别名（列别名 = 表别名 : 列名），可以据此来设置关联对象的字段值
            if (!column.contains(":")){ // 自身字段
                if(!column.endsWith('_') || ormMeta.columns.contains(column)) // 不是 中间表的外键字段别名, 用_后缀
                    setOriginal(column, value) // 设置本层字段值
            } else {// 关联对象字段
                if (value == null){ // 特殊处理null值(可能他值本身是null, 也可能left join没有匹配的行(关联对象为null)), 不能立即设置多层关联对象为null, 因为其他非null字段可能在后面, 因此先标记为可能为null
                    val name = column.substringBeforeLast(":") // 去掉最后一层, 即为关联对象名全路径
                    mayNullRelatedNames.add(name)
                } else  // 设置多层字段值
                    setOriginalMultiLevel(column, value)
            }
        }

        // 设置多层的关联对象为null, 防止后续获得关联属性时触发延迟查询
        setNullRelatesMultiLevel(mayNullRelatedNames)

        // 标记已加载
        loaded = true;
    }

    /**
     * 设置多层的关联对象为null
     *   mayNullRelatedNames只是可能而已, 如果 Orm::_data 中有设置过, 则表示不用再设为null
     *   如果不设关联对象为null, 则后续获得关联对象属性也会触发延迟查询, 浪费一条sql
     * @param mayNullRelatedNames 可能为null的多层关联对象名
     */
    protected fun setNullRelatesMultiLevel(mayNullRelatedNames: Set<String>){
        for(path in mayNullRelatedNames){
            val names = path.split(":")
            // 逐层检查关联对象
            var obj: OrmRelated = this
            for (name in names) {
                // 如果没有设置过关联对象, 则关联对象为null
                if(name !in obj._data){
                    obj._data[name] = null
                    continue
                }

                // 继续下一层
                obj = (obj._data[name] ?: continue) as OrmRelated
            }
        }
    }

    /**
     * 设置多层的原始的单个字段值
     * @param column 多层的字段名 = 表别名1 : 表别名2 : ... : 列名
     * @param value 字段值
     */
    protected fun setOriginalMultiLevel(column: String, value: Any?) {
        // 多层
        val cols = column.split(":")
        // 获得最后一层的关联对象
        var obj: OrmRelated = this
        for (i in 0..cols.size - 2) {
            obj = obj.getRelatedOrNew(cols[i], true) as OrmRelated; // 创建关联对象 + 标记为已加载
        }
        // 设置最底层的属性值
        obj.setOriginal(cols.last(), value)
    }

    /**
     * 从map中设置字段值
     *    对于关联对象字段值的设置: 只考虑一对一的关联对象, 不考虑一对多的关联对象
     *    场景: 1 读cache 2 从map中赋值然后保存
     *
     * @param from   字段值的哈希：<字段名 to 字段值>
     * @param include 要设置的字段名的列表
     * @param exclude 要排除的字段名的列表
     * @param includeRelated 是否包含关联属性, 仅当 include 为空时有效
     */
    public override fun fromMap(from: Map<String, Any?>, include: List<String>, exclude: List<String>, includeRelated: Boolean) {
        val columns = if (include.isEmpty())
                            if(includeRelated)
                                ormMeta.propsAndRelations // 包含关联属性, 但要先普通属性, 后关联属性, 因为后面代码 related() 获得关联对象要用到普通(外键)属性
                            else
                                ormMeta.props // 不包含关联属性
                        else
                            include

        for(column in columns){
            if(exclude.contains(column) // 排除的
                    || !from.containsKey(column)) // 没有的
                continue

            var value = from[column]
            if(value is Orm) // 被 fromOrm() 调用时, 属性值可能是Orm
                value = value._data

            if(value is Map<*, *>){ // 如果是map，则为关联对象
                val realValue = getRelatedOrNew(column) // 创建关联对象
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
     * 获得关联对象, 如果没有则查询
     *   原来是有参数指定查询列, 但为了优化联查sql的编译, 因此简化该参数
     *
     * @param name 关联对象名
     * @return
     */
    public override fun getRelatedOrQuery(name: String): Any? {
        if (name !in _data){
            // 获得关联关系
            val relation = ormMeta.getRelation(name)!!;

            // 根据关联关系来构建查询
            val query:OrmQueryBuilder? = relation.queryRelated(this) // 自动构建查询条件
            if(query == null) // 如果查询为空，说明主/外键为空，则数据有问题，则不查询不赋值（一般出现在调试过程中）
                return null;

            _data[name] = if (relation.isHasMany) // 查多个
                            query.findRows(transform = relation.modelRowTransformer)
                        else  // 查一个
                            query.findRow(transform = relation.modelRowTransformer)
        }

        return _data[name];
    }

    /**
     * 获得关联对象, 如果没有则创建新对象
     *
     * @param name 关联对象名
     * @param loaded 是否标记为已加载, 仅在查询db后设置原始字段值时调用
     * @return
     */
    public override fun getRelatedOrNew(name:String, loaded: Boolean): Any?{
        if (name !in _data){
            // 获得关联关系
            val relation = ormMeta.getRelation(name)!!;
            // 创建新对象
            val related = relation.newModelInstance()
            // 标记为已加载, 仅在查询db后设置原始字段值时调用
            if(loaded)
                related.loaded = true
            _data[name] = related;
        }

        return _data[name];
    }

    /**
     * 获得回调的关联对象
     *
     * @param name 关联对象名
     * @return
     */
    public override fun cbRelated(name: String): Any? {
        if (name !in _data){
            // 获得关联关系
            val relation = ormMeta.getCbRelation(name)!!;

            var result = relation.findAllRelated(this)
            _data[name] = if(relation.one2one)
                            result.firstOrNull()
                        else
                            result

        }

        return _data[name]
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
        // 删除关联对象
        return relation.deleteRelated(this, fkInMany)
    }

    /**
     * 添加关系（添加从表的外键值）
     *     一般用于添加 hasOne/hasMany 关系的从对象的外键值
     *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能添加本对象的外键咯
     *
     * @param name 关系名
     * @param value 外键值Any | 关联对象IOrm
     * @return
     */
    public override fun addRelation(name:String, value: Any): Boolean {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 添加关系
        return relation.addRelation(this, value)
    }

    /**
     * 删除关系，不删除关联对象，只是将关联的外键给清空
     *     一般用于清空 hasOne/hasMany 关系的从对象的外键值
     *     至于 belongsTo 关系的主对象中只要主键，没有外键，你只能清空本对象的外键咯
     *     关于外键的空值, 是外键字段的默认值(DbColumn.default), 如果没有设置字段默认值, 则为null
     *
     * @param name 关系名
     * @param fkInMany hasMany关系下的单个外键值Any|关联对象IOrm，如果为null，则删除所有关系, 否则删除单个关系
     * @return
     */
    public override fun removeRelations(name:String, fkInMany: Any?): Boolean {
        // 获得关联关系
        val relation = ormMeta.getRelation(name)!!;
        // 删除关系
        return relation.removeRelation(this, fkInMany)
    }

    /**
     * 添加 _data 中的 hasOne/hasMany 的关联关系
     *   仅用在 create/update() 方法中
     *   仅处理 _data 中改过的关联关系
     *
     */
    internal override fun addHasNRelations(){
        for((name, relation) in ormMeta.relations){
            // 仅处理 hasOne/hasMany 的关联关系
            if(relation.isBelongsTo)
                continue

            // 仅处理 _data 中改过的关联关系
            if(!isDirty(name))
                continue

            val value = _data[name]
            if(value != null)
                addRelation(name, value) // 添加关系
        }
    }

    /**
     * 删除 hasOne/hasMany 的关联关系, 由 delete() 触发
     *   要根据 `relation.cascadeDeleted` 来确定是删除对象(包含关系), 还是仅删除关系
     *
     */
    internal override fun removeHasNRelationsByDelete() {
        for(relation in ormMeta.hasNOrThroughRelations){
            val name = relation.name

            // 1 当级联删除时, 删除关联对象(包含关系)
            if(relation.cascadeDeleted) { // 级联删除
                val events = relation.ormMeta.processableEvents
                if(events.contains("beforeDelete") || events.contains("afterDelete")){ // 有删除的前置后置回调
                    // 为了能触发删除的前置后置回调，　因此使用 Orm.delete()　实现
                    // 查询关联对象
                    val related = getRelatedOrQuery(name)
                    // 逐个递归删除
                    when(related){
                        is Collection<*> -> (related as Collection<IOrm>).forEach{ it.delete(true) }
                        is IOrm -> related.delete(true)
                    }
                }else{ // 无删除的前置后置回调
                    deleteRelated(name) // 无法触发删除的前置后置回调
                }
                continue
            }

            // 2 仅删除关联关系
            removeRelations(name)
        }
    }

    /**
     * 删除 hasOne/hasMany 的关联关系, 由 update() 触发
     *   仅处理 _data 中改过的关联关系
     *
     */
    internal override fun removeHasNRelationsByUpdate() {
        for(relation in ormMeta.hasNOrThroughRelations){
            val name = relation.name

            // 仅处理 _data 中改过的关联关系
            if(!isDirty(name))
                continue

            // 如果关系的主键被修改过, 则中断关联关系的删除
            if(isDirty(relation.primaryProp))
                throw OrmException("Fail to remove model [${ormMeta.name}]'s `HasN` relations [${relation.primaryProp}]: primary key [${relation.primaryProp}] has been changed")

            // 仅删除关联关系
            removeRelations(name)
        }
    }

    /**
     * 查询关联表
     *    自动根据关联关系，来构建查询条件
     *
     * @param name 关系名
     * @param fkInMany hasMany关系下的单个外键值，如果为null，则更新所有关系, 否则更新单个关系
     * @return
     */
    fun queryRelated(name: String, fkInMany: Any? = null): OrmQueryBuilder?{
        // 获得关联关系
        val relation = ormMeta.getRelation(name)
        return relation?.queryRelated(this, fkInMany)
    }
}
