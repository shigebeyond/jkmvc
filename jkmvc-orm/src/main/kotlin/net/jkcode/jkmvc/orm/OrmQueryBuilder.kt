package net.jkcode.jkmvc.orm

import net.jkcode.jkmvc.db.DbResultRow
import net.jkcode.jkmvc.db.DbResultSet
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.orm.relation.*
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkmvc.query.DbQueryBuilder
import net.jkcode.jkmvc.query.IDbQueryBuilder
import net.jkcode.jkmvc.query.SqlAction
import net.jkcode.jkutil.common.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * 一对多的联查信息
 * @author shijianhang<772910474@qq.com>
 * @date 2020-06-26 9:52 AM
 */
data class WithInfo(
        public val columns: SelectColumnList?, // 查询列
        public val queryAction: ((OrmQueryBuilder)->Unit)? // 查询对象的回调函数, 只针对 hasMany 关系
)

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
open class OrmQueryBuilder(protected val ormMeta: IOrmMeta, // orm元数据
                           protected var convertingValue: Boolean = false, // 查询时是否智能转换字段值
                           protected var convertingColumn: Boolean = false, // 查询时是否智能转换字段名
                           protected var withSelect: Boolean = true, // with()联查时自动select关联表的字段
                           protected val listener: OrmQueryBuilderListener? = null // 事件处理
) : DbQueryBuilder(ormMeta.db) {

    init {
        from(ormMeta.table, ormMeta.name)
    }

    /**
     * 联查的关系，用于防止重复join同一个表
     */
    protected val joins:HashSet<String> = HashSet()

    /**
     * 关联查询hasMany的关系，需单独处理，不在一个sql中联查，而是单独一个sql查询
     * <hasMany关系名, 联查信息>
     */
    protected val withMany:MutableMap<String, WithInfo?> = HashMap()

    /**
     * 关联查询回调的关系名，需单独处理，不在一个sql中联查，而是单独调用回调来查询
     */
    protected val withCb:MutableList<String> = LinkedList()

    /**
     * 克隆对象
     * @return o
     */
    public override fun clone(): Any {
        val o = super.clone()
        // 复制复杂属性: 子句
        o.cloneProperties("joins", "withMany", "withCb")
        return o
    }

    /**
     * 清空联查信息
     *   不能在重写clear()时调用, 因为 withMany/withCb 的联查都是在查询完后再触发的
     * @return
     */
    protected fun clearWith() {
        joins.clear()
        withMany.clear()
        withCb.clear()
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
     * 联查多表, 支持多级路径
     *
     * @param paths 关联关系路径的数组, 支持多级路径, 用.分割
     * @return
     */
    public fun withPaths(vararg paths: String): OrmQueryBuilder {
        val cols = paths.mapToArray { path ->
            if(path.contains('.')){ // 多级路径
                val names = path.split('.').asReversed()
                var col: Any = emptyList<Any>()
                for (name in names){
                    col = name to col
                }
                col
            }else{ // 单级路径
                path
            }
        }

        return selectWiths(*cols)
    }

    /**
     * 联查单表
     *
     * @param name 关联关系名
     * @param select 是否select关联表的字段
     * @param columns 关联模型的字段列表
     * @param queryAction 查询对象的回调函数, 只针对 hasMany 关系
     * @return
     */
    public fun with(name: CharSequence, select: Boolean = withSelect, columns: SelectColumnList? = null, queryAction: ((OrmQueryBuilder)->Unit)? = null): OrmQueryBuilder {
        // select当前表字段
        if (selectColumns.isEmpty())
            select(ormMeta.name + ".*");

        // 处理回调关系
        val cbRelation = ormMeta.getCbRelation(name.toString())
        if(cbRelation != null){
            withCb.add(cbRelation.name)
            return this
        }

        // join关联表
        ormMeta.joinRelated(this, name, select, columns, queryAction = queryAction)

        return this
    }

    /**
     * 联查单表
     *
     * @param name 关联关系名
     * @param columns 关联模型的字段列表
     * @return
     */
    public fun with(name: CharSequence, columns: SelectColumnList?): OrmQueryBuilder {
        return with(name, withSelect, columns)
    }

    /**
     * 可select具体字段的 withs()
     *     设置查询字段，如果是关联字段，则联查
     *
     * @param columns 字段列表，其元素类型可以是 1 String 本模型字段名 2 RelatedSelectColumnList 关系名 + 关联模型的字段列表
     *               如("id", "name","org", "dept" to listOf("id", "title"), DbExpr("group", "group2") to listOf("*")), 其中本模型要显示id与name字段，org是关联模型名, 要显示所有字段, dept是关联模型名，要显示id与title字段, group是关联模型名, group2是别名
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
        // 查询本模型字段 -- 必须先于with()之前, 来select本模型对象字段, 否则with()会自动select *本模型全部字段
        columns.forEachMyColumns {
            select(ormMeta.name + '.' + convertColumn(it)); // 在每个select()处，智能转换字段名
        }

        // 联查关联模型
        columns.forEachRelatedColumns { name: CharSequence, columns: SelectColumnList? ->
            with(name, columns)
        }

        return this
    }

    /**
     * 联查joinMany关系的子关系
     *
     * @param name joinMany关系名
     * @param columns 查询字段
     * @param queryAction 查询对象的回调函数, 只针对 hasMany 关系
     * @return
     */
    public fun withMany(name: String, columns: SelectColumnList? = null, queryAction: ((OrmQueryBuilder)->Unit)?): OrmQueryBuilder {
        //数据结构：<hasMany关系名, 联查信息>
        withMany[name] = WithInfo(columns, queryAction)
        return this
    }

    /**
     * 添加联查的关系, 仅添加一次
     * @param name 关系名
     * @return 如果没有添加过则返回true, 否则false
     */
    internal fun addJoinOne(name: String): Boolean {
        return joins.add(name) // HashSet.add()仅添加一次
    }

    /**
     * 联查中间表
     *     中间表.外键 = 主表.主键
     *
     * @param slaveName 从表关系名
     * @return
     */
    public fun joinMiddleTable(slaveName: String): OrmQueryBuilder {
        return joinMiddleTable(ormMeta.getRelation(slaveName) as HasNThroughRelation)
    }

    /**
     * 联查中间表
     *     中间表.外键 = 主表.主键
     *
     * @param slaveRelation 从表关系
     * @return
     */
    public fun joinMiddleTable(slaveRelation: HasNThroughRelation): OrmQueryBuilder {
        val masterName = ormMeta.name

        // 准备条件
        val masterPk:DbKeyNames = slaveRelation.primaryKey.map {  // 主表.主键
            masterName + "." + it // masterName + "." + slaveRelation.primaryKey
        };
        val middleFk:DbKeyNames = slaveRelation.foreignKey.map { // 中间表.外键
            slaveRelation.middleTable + '.' + it // slaveRelation.middleTable + '.' + slaveRelation.foreignKey
        }

        // 查中间表
        return join(slaveRelation.middleTable).on(masterPk, middleFk) as OrmQueryBuilder// 中间表.外键 = 主表.主键
    }

    /**
     * 联查hasMany的表
     *   1. with()的限制
     *   一般而言, 通过 with() 方法联查的hasMany的关对象是分开一条sql来查询的, 这样才符合一对多关联关系的初衷
     *   譬如 user:employee 是 1:10, 查user是一条sql, 查employee是另外一条sql, 然后合并数据即可, 那么查user是1条记录, 查employee是10条记录, 可以很好的组装关联关系
     *   但如果查user/employee要合并为一条sql, 根据关系型db的笛卡尔积原理, 则会导致查user是10条记录, 查employee也是10条记录, 导致根本组装不了1:10的关联关系, 同查出的user是重复了10份
     *
     *   2.强制联查hasMany的表
     *   如果查询条件限制关联对象(如employee)记录只有一条, 那么可以使用该方法来强制联查关联表, 而不能使用 with()
     *
     * @param slaveName 从表关系名
     * @return
     */
    public fun joinHasMany(slaveName: String): OrmQueryBuilder{
        val relation = ormMeta.getRelation(slaveName)!!
        if(relation.isHasMany)
            relation.applyQueryJoinRelatedAndCondition(this, ormMeta.name, slaveName)

        return this
    }

    /**
     * 查找一个： select ... limit 1语句
     *
     * @param params 参数
     * @param transform 行转换函数
     * @return 单个数据
     */
    public override fun <T:Any> findRow(params: List<*>, db: IDb, transform: (DbResultRow) -> T): T?{
        val result = super.findRow(params, db, transform)
        // 联查hasMany
        if(result is IOrm){
            // 遍历每个hasMany关系的查询结果
            forEachManyQuery(result){ relation: IRelation, relatedItems:List<IOrm> ->
                // 设置关联属性
                result[relation.name] = relatedItems
            }

            // 遍历每个回调关系的查询结果
            forEachCbRelationQuery(result){ relation: ICbRelation<out IOrm, *, *>, relatedItems:List<*> ->
                // 设置关联属性
                result[relation.name] = if(relation.one2one)
                                            relatedItems.firstOrNull()
                                        else
                                            relatedItems
            }
        }

        // 在联查完后, 才清空联查信息
        clearWith()

        return result
    }

    /**
     * 查找多个： select 语句
     *
     * @param params 参数
     * @param transform 行转换函数
     * @return 列表
     */
    public override fun <T:Any> findRows(params: List<*>, db: IDb, transform: (DbResultRow) -> T): List<T>{
        val result = super.findRows(params, db, transform)
        if(result.isEmpty())
            return result

        // 联查hasMany
        if(result.first() is IOrm){
            val items = result as List<IOrm>

            // 遍历每个hasMany关系的查询结果
            forEachManyQuery(result){ relation: IRelation, relatedItems:List<IOrm> ->
                relation.batchSetRelationProp(items, relatedItems)
            }

            // 遍历每个回调关系的查询结果
            forEachCbRelationQuery(result){ relation: ICbRelation<out IOrm, *, *>, relatedItems:List<*> ->
                relation.batchSetRelationProp(items as List<Nothing>, relatedItems as List<Nothing>)
            }
        }

        // 在联查完后, 才清空联查信息
        clearWith()

        return result
    }

    /**
     * 遍历每个hasMany关系的查询结果
     *
     * @param orm Orm对象或列表
     * @param action 处理关联查询结果的lambda，有2个参数: 1 relation 关联关系 2 查询结果
     */
    protected fun forEachManyQuery(orm:Any, action: ((relation: IRelation, relatedItems:List<IOrm>)-> Unit)){
        // 联查hasMany的关系
        for((name, withInfo) in withMany){
            val (columns, queryAction) = withInfo!!

            // 获得hasMany的关系
            val relation = ormMeta.getRelation(name)!!

            // 关联查询hasMany：自动构建查询条件
            val query = relation.queryRelated(orm)
            if(query == null)
                continue

            // 设置查询字段 + 递归联查子关系
            if(columns != null)
                query.selectWiths(columns)

            // 调用查询对象的回调
            if(queryAction != null)
                queryAction.invoke(query)

            // 得结果
            val relatedItems = query.findRows(transform = relation.modelRowTransformer)

            // 处理查询结果
            action(relation, relatedItems)
        }
    }


    /**
     * 遍历每个回调关系的查询结果
     *
     * @param orm Orm对象或列表
     * @param action 处理关联查询结果的lambda，有2个参数: 1 relation 关联关系 2 查询结果
     */
    protected fun forEachCbRelationQuery(orm:Any, action: ((relation: ICbRelation<out IOrm, *, *>, relatedItems:List<*>)-> Unit)){
        // 联查回调的关系
        for(name in withCb){
            // 获得回调的关系
            val relation = ormMeta.getCbRelation(name)!!

            // 关联查询
            val relatedItems = relation.findAllRelated(orm)

            // 处理查询结果
            action(relation, relatedItems)
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
    public fun selectRelated(relation: IRelation, relationName: String, columns: List<String>? = null, path: String = ""): OrmQueryBuilder {
        // 单独处理hasMany关系，不在一个sql中联查，而是单独查询
        if(relation.isHasMany){
            return this;
        }

        var cols: Collection<String>? = columns // 查询列
        var convertingColumn = this.convertingColumn // 是否转换字段名
        // 如果列为空 或 列只有*
        if(columns == null || columns.isEmpty() || columns.size == 1 && columns.first() == "*"){
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
    public inline fun convertColumn(prop: String): String {
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
    public inline fun convertValue(prop: String, value: Any?): Any? {
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
        val relation = ormMeta.getRelation(table)!!
        // 由关联模型来转
        return Pair(relation.ormMeta, column)
    }

    /**
     * 改写 execute(), 添加更新的事件处理
     */
    override fun execute(action: SqlAction, params: List<Any?>, generatedColumn: String?, db: IDb): Long {
        // 更新的前置处理
        listener?.beforeExecute(this)
        // 执行更新
        val result = super.execute(action, params, generatedColumn, db)
        // 更新的后置处理
        listener?.afterExecute(this)
        return result
    }

    /**
     * 改写 findResult(), 添加查询的事件处理
     */
    override fun <T> findResult(params: List<*>, single: Boolean, db: IDb, transform: (DbResultSet) -> T): T {
        // 查询的前置处理
        listener?.beforeFind(this)
        // 查询
        val result = super.findResult(params, single, db, transform)
        // 查询的后置处理
        listener?.afterFind(this)
        return result
    }

}
