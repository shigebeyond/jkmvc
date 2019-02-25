package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.common.isArrayOrCollection
import net.jkcode.jkmvc.common.isNullOrEmpty
import net.jkcode.jkmvc.common.iteratorArrayOrCollection
import net.jkcode.jkmvc.common.map
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.query.DbQueryBuilder
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.query.IDbQueryBuilder
import net.jkcode.jkmvc.db.Row
import java.util.*
import kotlin.collections.HashMap

/**
 * 面向orm对象的sql构建器
 *    当前表与关联表都带别名
 *    当前表的别名=ormMeta.name
 *    关联表的别名=关联关系名
 *
 * @author shijianhang
 * @date 2016-10-16 下午8:02:28
 *
 */
open class OrmQueryBuilder(protected val ormMeta: IOrmMeta /* orm元数据 */,
                      protected var convertingValue: Boolean = false /* 查询时是否智能转换字段值 */,
                      protected var convertingColumn: Boolean = false /* 查询时是否智能转换字段名 */,
                      protected var withSelect: Boolean = true /* with()联查时自动select关联表的字段 */
) : DbQueryBuilder(ormMeta.db) {

    init {
        from(ormMeta.table, ormMeta.name)
    }

    /**
     * 关联查询的记录，用于防止重复join同一个表
     */
    protected val joins:MutableMap<String, IRelationMeta> = HashMap()

    /**
     * 关联查询hasMany的关系，需单独处理，不在一个sql中联查，而是单独查询
     * <hasMany关系名, [子关系名+子关系字段]>
     */
    protected val joinMany:MutableMap<String, SelectColumnList?> = HashMap()

    /**
     * 清空条件
     * @return
     */
    public override fun clear(): IDbQueryBuilder {
        joins.clear()
        joinMany.clear()
        return super.clear()
    }

    /**
     * 切换智能转换的模式
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @return
     */
    public fun toggleConvertPattern(convertingValue: Boolean, convertingColumn: Boolean): OrmQueryBuilder{
        this.convertingColumn = convertingColumn
        this.convertingValue = convertingValue
        return this
    }

    /********************************* with系列方法，用于实现关联对象查询 **************************************/
    /**
     * 联查多表
     *
     * @param names 关联关系名的数组
     * @return
     */
    public fun withs(vararg names: String): OrmQueryBuilder {
        for(name in names)
            with(name)
        return this
    }

    /**
     * 联查单表
     *
     * @param name 关联关系名
     * @param select 是否select关联表的字段
     * @param columns 关联模型的字段列表
     * @return
     */
    public fun with(name: String, select: Boolean = withSelect, columns: SelectColumnList? = null): OrmQueryBuilder {
        // select当前表字段
        if (select && selectColumns.isEmpty())
            select(ormMeta.name + ".*");

        // join关联表
        ormMeta.joinRelated(this, name, select, columns)

        return this
    }

    /**
     * 联查单表
     *
     * @param name 关联关系名
     * @param columns 关联模型的字段列表
     * @return
     */
    public fun with(name: String, columns: SelectColumnList?): OrmQueryBuilder {
        return with(name, withSelect, columns)
    }

    /**
     * 可select具体字段的 withs()
     *     设置查询字段，如果是关联字段，则联查
     *
     * @param columns 字段列表，其元素类型可以是 1 String 本模型字段名 2 RelatedSelectColumnList = Pair<String, SelectColumnList?> 关系名 + 关联模型的字段列表
     *               如("id", "name", "dept" to listOf("id", "title")), 其中本模型要显示id与name字段，dept是关联模型，要显示id与title字段
     * @return
     */
    public fun selectWiths(vararg columns: Any): OrmQueryBuilder {
        val selects = SelectColumnList.parse(ormMeta, columns)
        return selectWiths(selects)
    }

    /**
     * 可select具体字段的 withs()
     *     设置查询字段，如果是关联字段，则联查
     *
     * @param columns 字段列表
     * @return
     */
    public fun selectWiths(columns: SelectColumnList): OrmQueryBuilder {
        // 联查关联模型
        columns.forEachRelatedColumns { name, columns ->
            with(name, columns)
        }

        // 查询本模型字段
        columns.forEachMyColumns {
            select(ormMeta.name + '.' + convertColumn(it)); // 在每个select()处，智能转换字段名
        }
        return this
    }

    /**
     * 联查joinMany关系的子关系
     *
     * @param name joinMany关系名
     *
     * @return
     */
    public fun withMany(name: String, columns: SelectColumnList? = null): OrmQueryBuilder {
        //数据结构：<hasMany关系名, 查询字段>
        joinMany[name] = columns
        return this
    }

    /**
     * 联查从表
     *     从表.外键 = 主表.主键
     *
     * @param master 主表模型
     * @param masterName 主表别名
     * @param slaveRelation 从表关系
     * @param slaveName 从表别名
     * @return
     */
    public fun joinSlave(master: IOrmMeta, masterName:String, slaveRelation: IRelationMeta, slaveName: String): OrmQueryBuilder {
        // 检查并添加关联查询记录
        if(joins.containsKey(slaveName))
            return this;

        joins[slaveName] = slaveRelation

        // 准备条件
        val masterPk:DbKeyNames = slaveRelation.primaryKey.map{  // 主表.主键
            masterName + "." + it // masterName + "." + slaveRelation.primaryKey
        };

        val slave = slaveRelation.ormMeta
        val slaveFk:DbKeyNames = slaveRelation.foreignKey.map {  // 从表.外键
            slaveName + "." + it // slaveName + "." + slaveRelation.foreignKey
        }

        // 查从表
        return join(DbExpr(slave.table, slaveName), "LEFT").on(slaveFk, "=", masterPk) as OrmQueryBuilder; // 从表.外键 = 主表.主键
    }

    /**
     * 通过中间表联查从表
     *     中间表.外键 = 主表.主键
     *     中间表.远端外键 = 从表.远端主键
     *
     * @param master 主表模型
     * @param masterName 主表别名
     * @param slaveRelation 从表关系
     * @param slaveName 从表别名
     * @return
     */
    public fun joinSlaveThrough(master: IOrmMeta, masterName:String, slaveRelation: MiddleRelationMeta, slaveName: String): OrmQueryBuilder {
        // 检查并添加关联查询记录
        if(joins.containsKey(slaveName))
            return this;

        joins[slaveName] = slaveRelation

        // 准备条件
        val masterPk:DbKeyNames = slaveRelation.primaryKey.map {  // 主表.主键
            masterName + "." + it // masterName + "." + slaveRelation.primaryKey
        };
        val middleFk:DbKeyNames = slaveRelation.foreignKey.map { // // 中间表.外键
            slaveRelation.middleTable + '.' + it // slaveRelation.middleTable + '.' + slaveRelation.foreignKey
        }

        val slave = slaveRelation.ormMeta
        val slavePk2:DbKeyNames = slaveRelation.farPrimaryKey.map { // 从表.远端主键
            slaveName + "." + it // slaveName + "." + slaveRelation.farPrimaryKey
        }
        val middleFk2:DbKeyNames = slaveRelation.farForeignKey.map {  // 中间表.远端外键
            slaveRelation.middleTable + '.' + it // slaveRelation.middleTable + '.' + slaveRelation.farForeignKey
        }

        // 查中间表
        join(slaveRelation.middleTable).on(masterPk, "=", middleFk) // 中间表.外键 = 主表.主键

        // 查从表
        return join(DbExpr(slave.table, slaveName), "LEFT").on(slavePk2, "=", middleFk2) as OrmQueryBuilder; // 中间表.远端外键 = 从表.远端主键
    }

    /**
     * 联查主表
     *     主表.主键 = 从表.外键
     *
     * @param slave 从表模型
     * @param slaveName 从表别名
     * @param masterRelation 主表关系
     * @param masterName 主表别名
     * @return
     */
    public fun joinMaster(slave: IOrmMeta, slaveName:String, masterRelation: IRelationMeta, masterName: String): OrmQueryBuilder {
        // 检查并添加关联查询记录
        if(joins.containsKey(masterName))
            return this;

        joins[masterName] = masterRelation

        // 准备条件
        val slaveFk:DbKeyNames = masterRelation.foreignKey.map { // 从表.外键
            slaveName + "." + it // slaveName + "." + masterRelation.foreignKey
        }

        val master: IOrmMeta = masterRelation.ormMeta;
        val masterPk:DbKeyNames = masterRelation.primaryKey.map {  // 主表.主键
            masterName + "." + it //masterName + "." + masterRelation.primaryKey
        }

        // 查主表
        return join(DbExpr(master.table, masterName), "LEFT").on(masterPk, "=", slaveFk) as OrmQueryBuilder; // 主表.主键 = 从表.外键
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param transform 转换函数
     * @return 单个数据
     */
    public override fun <T:Any> find(params: List<Any?>, db: IDb, transform: (Row) -> T): T?{
        val result = super.find(params, db, transform);
        // 联查hasMany
        if(result is Orm){
            // 遍历每个hasMany关系的查询结果
            forEachManyQuery(result){ name:String, relation:IRelationMeta, relatedItems:List<IOrm> ->
                // 设置关联属性
                result[name] = relatedItems
            }
        }
        return result
    }

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param transform 转换函数
     * @return 列表
     */
    public override fun <T:Any> findAll(params: List<Any?>, db:IDb, transform: (Row) -> T): List<T>{
        val result = super.findAll(params, db, transform);
        if(result.isEmpty())
            return result

        // 联查hasMany
        if(result.first() is Orm){
            val items = result as List<IOrm>

            // 遍历每个hasMany关系的查询结果
            forEachManyQuery(result){ name:String, relation:IRelationMeta, relatedItems:List<IOrm> ->
                setHasManyProp(items, name, relation, relatedItems)
            }
        }
        return result
    }

    /**
     * 设置hasMany关系的属性值
     *
     * @param items 本模型对象
     * @param name 关系名
     * @param relation 关联关系
     * @param relatedItems 关联模型对象
     */
    protected fun setHasManyProp(items: List<IOrm>, name: String, relation: IRelationMeta, relatedItems: List<IOrm>) {
        if(items.isEmpty())
            return

        if(relatedItems.isEmpty()){
            // 设置关联属性为空list
            for (item in items)
                item[name] = emptyList<Any>()
            return
        }

        // 检查主外键的类型: 数据库中主外键字段类型可能不同，则无法匹配
        val primaryProp = relation.primaryProp // 主表.主键
        val foreignProp = if (relation is MiddleRelationMeta)
            relation.middleForeignProp // 中间表.外键
        else
            relation.foreignProp // 从表.外键
        val firstPk:DbKeyValues = items.first().gets(primaryProp)
        val firstFk:DbKeyValues = relatedItems.first().gets(foreignProp)
        if (firstPk::class != firstFk::class) {
            throw OrmException("模型[${ormMeta.name}]联查[${name}]的hasMany类型的关联对象失败: 主键[${ormMeta.table}.${relation.primaryKey}]字段类型[${firstPk::class}]与外键[${relation.model.modelOrmMeta.table}.${relation.foreignKey}]字段类型[${firstFk::class}]不一致，请改成一样的")
        }

        // 设置关联属性 -- 双循环匹配主外键
        for (item in items) { // 遍历每个源对象，收集关联对象
            val myRelated = LinkedList<IOrm>()
            for (relatedItem in relatedItems) { // 遍历每个关联对象，进行匹配
                // hasMany关系的匹配：主表.主键 = 从表.外键
                val pk:DbKeyValues = item.gets(primaryProp) // 主表.主键
                val fk:DbKeyValues = relatedItem.gets(foreignProp) // 从表.外键
                if (pk == fk) // DbKey.equals()
                    myRelated.add(relatedItem)
            }
            item[name] = myRelated
        }

        // 清空列表
        (relatedItems as MutableList).clear()
    }

    /**
     * 遍历每个hasMany关系的查询结果
     *
     * @param orm Orm对象或列表
     * @param action 处理关联查询结果的lambda，有2个参数: 1 name 关联关系名 2 查询结果
     */
    protected fun forEachManyQuery(orm:Any, action: ((name:String, relation:IRelationMeta, relatedItems:List<IOrm>)-> Unit)){
        // 联查hasMany的关系
        for((name, columns) in joinMany){
            // 获得hasMany的关系
            val relation = ormMeta.getRelation(name)!!

            // 关联查询hasMany：自动构建查询条件
            val query = relation.queryRelated(orm)
            if(query == null)
                continue

            // 设置查询字段 + 递归联查子关系
            if(columns != null)
                query.selectWiths(columns)

            // 得结果
            val relatedItems = query.findAll(transform = relation.rowTransformer)

            // 处理查询结果
            action(name, relation, relatedItems)
        }
    }

    /**
     * select关联表的字段
     *
     * @param relation 关联关系
     * @param relationName 表别名
     * @param columns 查询的列
     * @param path 之前的路径
     */
    public fun selectRelated(relation: IRelationMeta, relationName: String, columns: List<String>? = null, path: String = ""): OrmQueryBuilder {
        // 单独处理hasMany关系，不在一个sql中联查，而是单独查询
        if(relation.type == RelationType.HAS_MANY){
            return this;
        }

        var cols = columns // 查询列
        var convertingColumn = this.convertingColumn // 是否转换字段名
        if(columns.isNullOrEmpty()){
            // 默认查全部列
            cols = relation.ormMeta.columns
            // 默认列，不转换字段名
            convertingColumn = false
        }

        // 构建列别名
        for (it in cols!!) {
            // 在每个select()处，智能转换字段名
            val col = if(convertingColumn) convertColumn(it) else it
            val columnAlias = path + ":" + col; // 列别名 = 表别名 : 列名，以便在设置orm对象字段值时，可以逐层设置关联对象的字段值
            val column = relationName + "." + col;
            select(DbExpr(column, columnAlias))
        }

        return this
    }

    /********************************* 改写父类的方法：支持自动转换字段名（多级属性名 => 字段名）与字段值（字符串类型的字段值 => 准确类型的字段值），不改写join()，请使用with()来代替 **************************************/
    /**
     * Creates a new "AND WHERE" condition for the query.
     *
     * @param   prop  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andWhere(prop: String, op: String, value: Any?): IDbQueryBuilder {
        return super.andWhere(convertColumn(prop), op, convertValue(prop, value))
    }

    /**
     * Creates a new "OR WHERE" condition for the query.
     *
     * @param   prop  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orWhere(prop: String, op: String, value: Any?): IDbQueryBuilder {
        return super.orWhere(convertColumn(prop), op, convertValue(prop, value))
    }

    /**
     * Applies sorting with "ORDER BY ..."
     *
     * @param   prop     column name or DbExpr
     * @param   direction  direction of sorting
     * @return
     */
    public override fun orderBy(prop: String, direction: String?): IDbQueryBuilder {
        return super.orderBy(convertColumn(prop), direction);
    }

    /**
     * Creates a "GROUP BY ..." filter.
     *
     * @param   prop  column name
     * @return
     */
    public override fun groupBy(prop: String): IDbQueryBuilder {
        return super.groupBy(convertColumn(prop))
    }

    /**
     * Creates a new "AND HAVING" condition for the query.
     *
     * @param   prop  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun andHaving(prop: String, op: String, value: Any?): IDbQueryBuilder {
        return super.andHaving(convertColumn(prop), op, convertValue(prop, value))
    }

    /**
     * Creates a new "OR HAVING" condition for the query.
     *
     * @param   prop  column name or DbExpr
     * @param   op      logic operator
     * @param   value   column value
     * @return
     */
    public override fun orHaving(prop: String, op: String, value: Any?): IDbQueryBuilder {
        return super.orHaving(convertColumn(prop), op, convertValue(prop, value))
    }

    /**
     * 转换字段名：数据库中的字段名
     *
     * @param prop 属性名
     * @return 字段名
     */
    public fun convertColumn(prop: String): String {
        return if (convertingColumn)
            ormMeta.prop2Column(prop)
        else
            prop
    }

    /**
     * 智能转换属性值: 准确类型的值
     *
     * @param prop 属性名
     * @param value 属性值
     * @return 准确类型的属性值
     */
    public fun convertValue(prop: String, value: Any?): Any? {
        return if (convertingValue && (value is String || value.isArrayOrCollection()))
            convertIntelligent(prop, value!!)
        else
            value
    }

    /**
     * 智能转换字段值
     *
     * @param prop 多级属性
     * @param value
     * @return
     */
    protected fun convertIntelligent(prop: String, value: Any): Any? {
        // 1 解析出：关联模型 + 字段名
        val (model, column) = getModelAndColumn(prop)

        // 2 由关联模型来转
        // 2.1 多值
        val itr = value?.iteratorArrayOrCollection()
        if(itr != null){
            return itr.map {
                model.convertIntelligent(column, it as String)
            }
        }

        // 2.2 单值
        return model.convertIntelligent(column, value as String)
    }

    /**
     * 根据（最多2层的）属性获得关联模型与字段
     *
     * @param prop 多级属性
     * @return
     */
    protected fun getModelAndColumn(prop: String): Pair<IOrmMeta, String> {
        // 解析出：表名 + 字段名
        var table: String
        var column: String
        if (prop.contains('.')) {
            val arr = prop.split('.')
            table = arr[0]
            column = arr[1]
        } else {
            table = ormMeta.name
            column = prop
        }

        // 当前表
        if (table == ormMeta.name) {
            // 由当前模型来转
            return Pair(ormMeta, column)
        }

        // 关联表
        // 获得关联关系
        val relation = joins[table]!!
        // 由关联模型来转
        return Pair(relation.ormMeta, column)
    }

}
