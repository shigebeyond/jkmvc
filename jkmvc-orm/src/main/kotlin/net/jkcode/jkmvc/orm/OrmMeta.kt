package net.jkcode.jkmvc.orm

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.basic.DateConverter
import net.jkcode.jkmvc.db.*
import net.jkcode.jkmvc.orm.relation.*
import net.jkcode.jkmvc.orm.serialize.OrmConverter
import net.jkcode.jkmvc.query.CompiledSql
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkutil.cache.ICache
import net.jkcode.jkutil.collection.FixedKeyMapFactory
import net.jkcode.jkutil.common.*
import net.jkcode.jkutil.validator.IValidator
import net.jkcode.jkutil.validator.ModelValidateResult
import net.jkcode.jkutil.validator.ValidateResult
import java.lang.reflect.Constructor
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashSet
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * orm的元数据
 * 　两重职责
 *  1 模型映射表的映射元数据，如模型类/数据库/表名
 *  2 代理模型的属性读写
 *
 *  关于 cascadeDeleted:
 *      只对 hasOne/hasMany 有效, 对 belongsTo/hasOneThrough/hasManyThrough 无效
 *      对 belongsTo, 你敢删除 belongsTo 关系的主对象？
 *      对 hasOneThrough/hasManyThrough, 都通过中间表来关联了, 两者之间肯定是独立维护的, 只删除关联关系就好, 不删除关联对象
 */
open class OrmMeta(public override val model: KClass<out IOrm>, // 模型类
                   public override val label: String = model.modelName, // 模型中文名
                   public override var table: String = model.modelName, // 表名，假定model类名, 都是以"Model"作为后缀
                   public override var primaryKey: DbKeyNames = DbKeyNames("id"), // 主键
                   public override val cacheMeta: OrmCacheMeta? = null, // 缓存配置
                   public override val dbName: String = "default", // 数据库名
                   public override val pkEmptyRule: PkEmptyRule = PkEmptyRule.default, // 检查主键为空的规则
                   checkingTablePrimaryKey: Boolean = true // 是否检查表与主键是否存在
) : IJavaOrmMeta() {

    public constructor(
            model: KClass<out IOrm>, // 模型类
            label: String, // 模型中文名
            table: String, // 表名，假定model类名, 都是以"Model"作为后缀
            primaryKey: String, // 主键
            cacheMeta: OrmCacheMeta? = null, // 缓存配置
            dbName: String = "default", // 数据库名
            pkEmptyRule: PkEmptyRule = PkEmptyRule.default, // 检查主键为空的规则
            checkingTablePrimaryKey: Boolean = true // 是否检查表与主键是否存在
    ) : this(model, label, table, DbKeyNames(primaryKey), cacheMeta, dbName, pkEmptyRule, checkingTablePrimaryKey)

    /**
     * 给java调用
     */
    @JvmOverloads
    public constructor(
            model: Class<out IOrm>, // 模型类
            label: String = model.kotlin.modelName, // 模型中文名
            table: String = model.kotlin.modelName, // 表名，假定model类名, 都是以"Model"作为后缀
            primaryKey: DbKeyNames = DbKeyNames("id"), // 主键
            cacheMeta: OrmCacheMeta? = null, // 缓存配置
            dbName: String = "default", // 数据库名
            pkEmptyRule: PkEmptyRule = PkEmptyRule.default, // 检查主键为空的规则
            checkingTablePrimaryKey: Boolean = true // 是否检查表与主键是否存在
    ): this(model.kotlin, label, table, primaryKey, cacheMeta, dbName, pkEmptyRule, checkingTablePrimaryKey)

    /**
     * 给java调用
     */
    @JvmOverloads
    public constructor(
            model: Class<out IOrm>, // 模型类
            label: String, // 模型中文名
            table: String, // 表名，假定model类名, 都是以"Model"作为后缀
            primaryKey: String, // 主键
            cacheMeta: OrmCacheMeta? = null, // 缓存配置
            dbName: String = "default", // 数据库名
            pkEmptyRule: PkEmptyRule = PkEmptyRule.default, // 检查主键为空的规则
            checkingTablePrimaryKey: Boolean = true // 是否检查表与主键是否存在
    ) : this(model, label, table, DbKeyNames(primaryKey), cacheMeta, dbName, pkEmptyRule, checkingTablePrimaryKey)

    /**
     * 无参数的构造函数
     */
    protected var constructorNoarg: Constructor<out IOrm>? = null

    /**
     * 可变参数的构造函数
     *   vararg pk?: Any
     */
    protected var constructorVararg: Constructor<out IOrm>? = null

    /**
     * 缓存
     *    对每个模型类都可以指定 cacheMeta 来使用本地缓存or外部缓存
     */
    private val cache: ICache by lazy {
        ICache.instance(cacheMeta!!.cacheType)
    }

    init {
        if(checkingTablePrimaryKey) {
            // 检查表
            val dbTable = db.getTable(table)
            if (dbTable == null)
                throw OrmException("Table [$table] not exist")

            // 检查主键
            if (!dbTable.hasColumns(primaryKey))
                throw OrmException("Table [$table] miss `primaryKey` columns: $primaryKey")
        }

        // 检查 model 类的默认构造函数
        constructorNoarg = model.java.getConstructorOrNull() // 无参数构造函数, 其中 GeneralModel 有个internal的无参构造函数
        if(constructorNoarg == null)
            constructorVararg = model.java.getConstructorOrNull(Array<Any>::class.java) // 可变参数构造函数
        if (constructorNoarg == null && constructorVararg == null)
            throw OrmException("Model Class [$model] has no no-arg constructor") // Model类${clazz}无默认构造函数

        // 关联 缓存配置 与 元数据
        cacheMeta?.ormMeta = this

        // 一开始缓存全部
        if (cacheMeta != null && cacheMeta!!.initAll) {
            val query = queryBuilder()
            // 缓存时联查
            cacheMeta!!.applyQueryWiths(query)

            // 查询并缓存
            val items = query.findRows {
                val item = newInstance()
                item.setOriginal(it)
                item
            }
            for (item in items){
                val key = getCacheKey(item.pk)
                cache.put(key, item)
            }
        }
    }

    companion object {

        /**
         * orm配置
         */
        public val config: Config = Config.instance("orm")
    }

    /**
     * 模型名
     */
    public override val name: String = model.modelName

    /**
     * 主键属性
     */
    public override val primaryProp: DbKeyNames = columns2Props(primaryKey)

    /**
     * 关联关系
     */
    public override val relations: MutableMap<String, IRelation> by lazy {
        HashMap<String, IRelation>()
    }

    /**
     * 通过回调动态获得对象的关联关系
     */
    public val cbRelations: MutableMap<String, ICbRelation<out IOrm, *, *>> by lazy {
        HashMap<String, ICbRelation<out IOrm, *, *>>()
    }

    /**
     * 每个字段的校验规则
     */
    public override val rules: MutableMap<String, IValidator> by lazy {
        HashMap<String, IValidator>()
    };

    /**
     * 能处理的序列化事件
     *   就是在Orm子类中重写了事件处理函数
     */
    public override val processableEvents: List<String> by lazy {
        "beforeCreate|afterCreate|beforeUpdate|afterUpdate|beforeSave|afterSave|beforeDelete|afterDelete".split('|').filter { event ->
            //val method = model.declaredFunctions.any { it.name == event } // declaredFunctions只是获得当前类声明的方法, 但有可能在父类中定义了事件函数
            val method = model.java.getMethod(event)
            //method.declaringClass != OrmValid::class.java // 实际上事件处理方法是定义在 IOrmPersistent 接口, 但反射中获得声明类却是 OrmValid 抽象类
            method.declaringClass.isSubClass(Orm::class.java) // 简写: 只要是orm子类即可
        }
    }

    /**
     * 获得实体类: 模型类实现 IEntitiableOrm 接口时, 指定的泛型类型
     */
    public override val entityClass: Class<*>? by lazy {
        if (model.isSubclassOf(IEntitiableOrm::class))
            model.java.getInterfaceGenricType(IEntitiableOrm::class.java)
        else
            null
    }

    /**
     * 数据库
     *   OrmMeta 会被多个请求访问, 因此每次都读最新的db
     *   不能直接赋值, 否则持有的db被上一个请求释放了, 下一个请求再用就报错
     */
    public override val db: IDb
        get() = Db.instance(dbName)

    /**
     * 默认外键
     */
    public override val defaultForeignKey: DbKeyNames by lazy {
        primaryKey.wrap(table + '_')  // table + '_' + primaryKey;
    }

    /**
     * db表
     */
    override val dbTable: DbTable
        get() = db.getTable(table)!!

    /**
     * db列
     *   不用 by lazy, 因为在jkerp会子类改写
     */
    override val dbColumns: Map<String, DbColumn>
        get(){
            return db.getColumnsByTable(table)
        }

    /**
     * 表字段
     */
    public override val columns: Collection<String> by lazy {
        dbColumns.keys
    }

    /**
     * 对象属性
     */
    public override val props: List<String> by lazy {
        columns.map {
            column2Prop(it)
        }
    }

    /**
     * 主键之外的属性名
     */
    public override val propsExcludePk: List<String> by lazy{
        props - primaryProp.columns
    }

    /**
     * 对象属性名+关系名
     */
    public override val propsAndRelations: List<String> by lazy {
        props + relations.keys + cbRelations.keys
    }

    /**
     * 要序列化的对象属性
     *   写时序列化, 读时反序列化
     */
    public override val serializingProps: List<String> = emptyList()

    /**
     * 如果是空字符串转为null的外键属性
     */
    public override val emptyToNullForeignProps: List<String> by lazy {
        if (config["foreignPropEmptyToNull"]!!) {
            val props = ArrayList<String>()
            for ((name, relation) in relations) {
                if (relation.isBelongsTo) { // 只对belongsTo有效, 表示本模型有外键
                    props.addAll(relation.foreignProp.columns)
                }
            }
            props
        } else
            emptyList<String>()
    }

    /**
     * 创建时间
     */
    public override val createdDateProp: String? by lazy {
        getExistProp(config["createdDateProp"])
    }

    /**
     * 创建人id
     */
    public override val createdByProp: String? by lazy {
        getExistProp(config["createdByProp"])
    }

    /**
     * 创建人名
     */
    public override val createdByNameProp: String? by lazy {
        getExistProp(config["createdByNameProp"])
    }

    /**
     * 修改时间
     */
    public override val modifiedDateProp: String? by lazy {
        getExistProp(config["modifiedDateProp"])
    }

    /**
     * 修改人id
     */
    public override val modifiedByProp: String? by lazy {
        getExistProp(config["modifiedByProp"])
    }

    /**
     * 修改人名
     */
    public override val modifiedByNameProp: String? by lazy {
        getExistProp(config["modifiedByNameProp"])
    }

    /**
     * 获得存在的属性, 如不存在则返回null
     * @param prop
     * @return
     */
    protected fun getExistProp(prop: String?): String? {
        if (!prop.isNullOrEmpty() && props.contains(prop))
            return prop

        return null
    }

    /**
     * 对象属性名+关系名+中间表外键
     */
    public val propsAndRelationsAndmiddleForeignProps: HashSet<String>
        get() {
            // 之所以用 HashSet, 是因为可能有多个中间表的关联关系, 进而有重名的中间表外键属性
            val keys = HashSet<String>(props.size + relations.size)
            keys.addAll(props) // 1 对象属性
            keys.addAll(relations.keys) // 2 关联属性
            for ((_, relation) in relations) { // 3 有中间表关联关系: 中间表的外键属性
                if (relation is HasNThroughRelation)
                    keys.addAll(relation.middleForeignProp.columns)
            }
            keys.addAll(cbRelations.keys) // 4 回调的关联属性
            return keys
        }

    /**
     * 数据的工厂
     */
    public override val dataFactory: FixedKeyMapFactory by lazy {
        FixedKeyMapFactory(false, *propsAndRelationsAndmiddleForeignProps.toTypedArray())
    }

    /**
     * 智能转换字段值
     *    在不知字段类型的情况下，将string赋值给属性
     *    => 需要将string转换为属性类型
     *    => 需要显式声明属性
     *
     * @param column
     * @param value 字符串
     * @return
     */
    public override fun convertIntelligent(column: String, value: String): Any? {
        // 1 获得属性
        val prop = model.getInheritProperty(column)
        if (prop == null)
            throw OrmException("Model Class ${model} has no property: $column");

        // 2 转换类型
        return value.to(prop.getter.returnType)
    }

    /**
     * 如果有要处理的事件(只是增删改, 不包含查)，则开启事务
     *
     * @param events 多个事件名，以|分隔，如 beforeCreate|afterCreate
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @param statement
     * @return
     */
    public override fun <T> transactionWhenHandlingEvent(events: String, withHasRelations: Boolean, statement: () -> T): T {
        val needTrans = hasHandleAnyEvent(events) || withHasRelations
        return db.transaction(!needTrans, statement)
    }

    /**
     * 创建实例
     *   使用无参数构造函数/可变参数构造参数来实例化
     * @return
     */
    public override fun newInstance(): IOrm {
        return constructorNoarg?.newInstance() // 无参数构造函数
                ?: constructorVararg?.newInstance(emptyArray<Any>()) // 可变参数构造参数
                ?: throw OrmException("Model Class [$model] has no no-arg constructor") // Model类${clazz}无默认构造函数
    }

    /********************************* 校验 **************************************/
    /**
     * 添加规则
     * @param field
     * @param rule
     * @return
     */
    public override fun addRule(field: String, rule: IValidator): OrmMeta {
        rules[field] = rule;
        return this;
    }

    /**
     * 校验orm对象数据
     * @param item
     * @return
     */
    public override fun validate(item: IOrmEntity): ModelValidateResult? {
        if(rules.isEmpty())
            return null

        // 逐个属性校验
        val errors = HashMap<String, String>()
        for ((field, rule) in rules) {
            // 只处理脏字段
            if(item is IOrm && !item.isDirty(field))
                continue

            // 1 获得属性值
            val value: Any = item[field];

            // 2 校验单个属性: 属性值可能被修改
            val result = rule.validate(value, (item as OrmEntity).getData())

            // 3 校验失败, 记录错误
            if (result.error != null) {
                errors[field] = result.error as String
                continue
            }

            // 4 校验成功, 更新被修改的属性值
            if (value !== result.value)
                item[field] = result.value;
        }

        return ValidateResult(item, errors, name)
    }

    /********************************* 缓存 **************************************/
    /**
     * 根据主键值来删除缓存
     * @param item
     */
    public override fun removeCache(item: IOrm) {
        removeCache(item, true)
    }

    /**
     * 根据主键值来删除缓存
     * @param item
     * @param removingRelated 是否删除关联对象的缓存
     */
    protected fun removeCache(item: IOrm, removingRelated: Boolean) {
        // 删除关联对象的缓存
        if(removingRelated)
            removeRelatedCache(item)

        // 删除本对象的缓存
        if (cacheMeta != null) {
            val key = getCacheKey(item.pk)
            cache.remove(key)
            dbLogger.debug("Remove orm [{}] cache: {}", model.qualifiedName, key)
        }
    }

    /**
     * 连带缓存当前模型的关联关系
     */
    protected val relationsCacheThis: List<IRelation> by lazy{
        // 遍历每个关联关系, 看看他有没有缓存, 并且连带缓存当前模型
        relations.values.filter { relation ->
            val relatedMeta = relation.ormMeta
            // 关联对象 连带缓存了当前模型
            val hasCacheThis = relatedMeta.cacheMeta?.hasWithRelation(this)
                    ?: false
            hasCacheThis
        }
    }

    /**
     * 删除关联对象的缓存
     * @param item
     */
    fun removeRelatedCache(item: IOrm){
        // 遍历每个(缓存当前模型的)关联关系
        for(relation in relationsCacheThis){
            // 获得关联对象
            val related: IOrm? = item.get(relation.name)
            if(related == null)
                continue

            // 删除关联对象的缓存
            val relatedMeta = relation.ormMeta
            relatedMeta.removeCache(related, false) // 递归调用, 但第二个参数表示只递归一层
        }
    }

    /**
     * 读缓存, 无则读db
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     * @param expires 缓存过期时间
     * @return
     */
    public override fun <T : IOrm> getOrPutCache(pk: DbKeyValues, item: T?, expires: Long): T? {
        // 如果主键为空, 则直接返回null, 不抛异常, 因为调用端可能需要根据返回值做检查(如existByPk()), 抛异常不合适
        if (isPkEmpty(pk))
            return null

        // 无需缓存
        if (cacheMeta == null)
            return innerloadByPk(pk, item)

        // 读缓存, 无则读db
        val cacheItem = innerGetOrPutCache(pk, item, expires)
        if (cacheItem == null) // 无记录
            return null
        if (item == null) // 无赋值对象, 则返回缓存对象
            return cacheItem

        // 有赋值对象, 则赋值并返回
        item.fromMap((cacheItem as Orm).getData())
        item.loaded = true
        return item
    }

    /**
     * 读缓存, 无则读db
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     * @param expires 缓存过期时间
     * @return
     */
    private fun <T : IOrm> innerGetOrPutCache(pk: DbKeyValues, item: T?, expires: Long): T? {
        // 读缓存, 无则读db
        val key = getCacheKey(pk)
        return cache.getOrPut(key, expires) {
            // 读db
            val item = innerloadByPk(pk, item)
            // 直接缓存Orm, 其序列化依靠 OrmEntityFstSerializer
            item
        }.get() as T?
    }

    /**
     * 获得缓存的key
     * @param pk
     * @return
     */
    protected fun getCacheKey(pk: DbKeyValues): String {
        return pk.columns.joinToString("_", "orm:$dbName:$name:")
    }

    /********************************* query builder **************************************/
    /**
     * 处理 queryBuilder() 返回的查询对象的事件
     *   主要用于给子类重载, 以便对子类 queryBuilder 做全局的配置, 如添加全局的where条件
     */
    protected open val queryListener: OrmQueryBuilderListener? = null

    /**
     * 复用的查询构建器, 线程安全
     */
    protected val reusedQueryBuilders: ThreadLocal<OrmQueryBuilder> = ThreadLocal.withInitial {
        queryBuilder();
    }

    /**
     * 是否使用预编译sql来做常规查询, 以便提升编译性能
     *    预编译sql: selectSqlByPk/deleteSqlByPk/getInsertSql()/getUpdateSql()
     *    仅用于常规查询: OrmPersistent 中的 loadByPk()/delete()/create()/update()
     */
    override val precompileSql: Boolean = true

    /**
     * 根据主键查询的sql
     *   OrmQueryBuilder 是联查时用
     */
    override val selectSqlByPk: Pair<CompiledSql, OrmQueryBuilder> by lazy{
        val query = reuseQueryBuilder()
        // 缓存时联查
        if (cacheMeta != null) {
            cacheMeta!!.applyQueryWiths(query)
        }
        // 查询主键
        val pk = primaryKey.map {
            DbExpr.question
        }
        val sql = query.where(primaryKey, pk)
                .compileSelectOne()
        sql to query
    }

    /**
     * 根据主键删除的sql
     */
    override val deleteSqlByPk: CompiledSql by lazy{
        val pk = primaryKey.map {
            DbExpr.question
        }
        reuseQueryBuilder()
            .where(primaryKey, pk) // 查询主键
            .compileDelete()
    }

    /**
     * <插入字段 to 插入sql>
     */
    protected val insertSqls: ConcurrentHashMap<BitSet, CompiledSql> = ConcurrentHashMap()

    /**
     * 获得插入的sql
     * @param insertProps 用属性, 而非列, 是因为 props2bitSet() 不用转(dataFactory只认识属性), 而 getOrPut() 中就算要转为列也是一次性的
     * @return
     */
    override fun getInsertSql(insertProps: Collection<String>): CompiledSql{
        return getSqlByProps(insertProps, insertSqls){
            val colums = insertProps.mapToArray { prop ->
                prop2Column(prop)
            }
            val values = insertProps.mapToArray {
                DbExpr.question
            }
            reuseQueryBuilder()
                    .insertColumns(*colums) // 列
                    .value(*values) // 值
                    .compileInsert()
        }!!
    }

    /**
     * <更新字段 to 更新sql>
     */
    protected val updateSqls: ConcurrentHashMap<BitSet, CompiledSql> = ConcurrentHashMap()

    /**
     * 获得更新的sql
     * @param insertProps 用属性, 而非列, 是因为 props2bitSet() 不用转(dataFactory只认识属性), 而 getOrPut() 中就算要转为列也是一次性的
     * @return
     */
    override fun getUpdateSql(updateProps: Collection<String>): CompiledSql{
        return getSqlByProps(updateProps, updateSqls){
            val pk = primaryKey.map {
                DbExpr.question
            }
            val query = reuseQueryBuilder()
                    .where(primaryKey, pk) // 过滤主键
            for (prop in updateProps) {
                val col = prop2Column(prop)
                query.set(col, DbExpr.question) // 更新字段
            }
            query.compileUpdate()
        }!!
    }

    /**
     * 复用BitSet
     */
    protected val reusedBitSets: ThreadLocal<BitSet> = ThreadLocal.withInitial {
        BitSet()
    }

    /**
     * 多属性转位集
     *    dataFactory 的key是属性, 而非列
     * @param props 属性
     * @param reused 是否复用 BitSet
     * @return
     */
    protected fun props2bitSet(props: Collection<String>, reused: Boolean): BitSet {
        val bs: BitSet
        if(reused){
            bs = reusedBitSets.get() // 复用 BitSet
            bs.clear()
        }else {
            bs = BitSet()
        }
        for (prop in props){
            bs.set(dataFactory.keyIndex(prop))
        }
        return bs
    }

    /**
     * 根据属性来获得缓存的编译sql
     *   为减少 BitSet 创建, 复用 BitSet
     *
     * @param props 多属性
     * @param sqls 缓存的sql
     * @param sqlFactory sql工厂,  如果不存在sql, 则创建
     */
    protected fun getSqlByProps(props: Collection<String>, sqls: ConcurrentHashMap<BitSet, CompiledSql>, sqlFactory: () -> CompiledSql): CompiledSql {
        // 收集字段的位集
        val bs = props2bitSet(props, true) // 复用 BitSet
        // 对复用 BitSet 不能使用 getOrPut, 因为 复用 BitSet 会被改变
        //return sqls.getOrPut(bs, sqlFactory)!!
        var sql = sqls[bs]
        if(sql == null){
            sql = sqlFactory()
            sql.paramNames = props.toList() // 记录参数(属性)顺序, 方便执行sql时按顺序拼接参数, 同时要复制props(不懂他是啥, 如Orm._dirty.keys, 会被清掉的)
            sqls.putIfAbsent(bs.clone() as BitSet, sql) // 设置key/value时, key必须用复制的 BitSet
        }
        return sql
    }

    /**
     * 获得orm查询构建器
     *
     * @param convertingValue 查询时是否智能转换字段值
     * @param convertingColumn 查询时是否智能转换字段名
     * @param withSelect with()联查时自动select关联表的字段
     * @return
     */
    public override fun queryBuilder(convertingValue: Boolean, convertingColumn: Boolean, withSelect: Boolean): OrmQueryBuilder {
        return OrmQueryBuilder(this, convertingValue, convertingColumn, withSelect, queryListener)
    }

    /**
     * 复用 QueryBuilder, 仅用在预编译sql中
     *    框架保证复用的OrmQueryBuilder实例是线程安全的
     */
    protected fun reuseQueryBuilder(): OrmQueryBuilder {
        val query = reusedQueryBuilders.get()
        // 不管三七二十一, clear()就对了, 保证复用对象干净, 有些OrmMeta子类(如SaasOrmMeta)会重写queryBuild()会带一些查询条件之类的
        query.clear()
        // 重置属性，特别是重置旧的已关闭的db
        query.reset(this.db, false, false, true)
        return query
    }

    /**
     * 根据主键值来加载数据
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     */
    protected fun <T : IOrm> innerloadByPk(pk: DbKeyValues, item: T? = null): T? {
        // 1 不预编译sql
        if(!precompileSql)
            return queryBuilder().where(primaryKey, pk).findRow {
                result2model(it, item)
            }

        // 2 使用预编译sql来查询
        val (sql, query) = selectSqlByPk
        val item = sql.findRow(pk.toList(), db) {
            result2model(it, item)
        }
        // 联查hasMany关系, 因为他是另外的sql
        if(item != null)
            query.rowQueryWithMany(item)
        return item
    }

    /**
     * db查询结果转模型实例
     */
    private fun <T : IOrm> result2model(it: DbResultRow, item: T?): T {
        //val result = item ?: model.java.newInstance() as T
        val result = item ?: newInstance() as T
        result.setOriginal(it)
        return result
    }

    /**
     * 根据主键值来加载数据
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     * @param useCache 是否使用缓存
     */
    public override fun loadByPk(pk: DbKeyValues, item: IOrm, useCache: Boolean) {
        if(useCache) { // 用缓存
            getOrPutCache(pk, item)
            return
        }

        // 不用缓存
        innerloadByPk(pk, item)
    }

    /**
     * 根据主键值来查找数据
     * @param pk 要查询的主键
     * @param useCache 是否使用缓存
     * @return
     */
    public override fun <T : IOrm> findByPk(pk: DbKeyValues, useCache: Boolean): T? {
        if(useCache) // 用缓存
            return getOrPutCache(pk)

        // 不用缓存
        return innerloadByPk(pk)
    }

    /**
     * 根据主键值来删除数据
     *    为了能触发删除的前置后置回调，　因此使用 Orm.delete()　实现
     *
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @param pk 要删除的主键
     * @return
     */
    public override fun deleteByPk(pk: DbKeyValues, withHasRelations: Boolean): Boolean {
        if (isPkEmpty(pk))
            return false

        val item = findByPk<IOrm>(pk, useCache = false)
        if (item != null && item.loaded)
            return item.delete(withHasRelations)

        return true
    }

    /**
     * 批量插入
     *   一般用于批量插入 OrmEntity 对象, 而不是 Orm 对象, 因此也不会触发 Orm 中的前置后置事件
     *
     * @param items
     * @return
     */
    public override fun batchInsert(items: List<IOrmEntity>): IntArray {
        if (items.isEmpty())
            return emptyIntArray()

        // 校验
        for (item in items) {
            validateOrThrow(item)
        }

        return db.transaction {
            // 前置
            for (item in items) {
                if (item is Orm)
                    item.triggerBeforeCreate()
            }

            // 构建insert语句
            // insert字段 -- 取全部字段, 不能取第一个元素的字段, 因为每个元素可能修改的字段都不一样, 这样会导致其他元素漏掉更新某些字段
            /*val props = (items.first() as OrmEntity).getDoc().keys
            val columns = props.mapToArray { prop ->
                prop2Column(prop)
            }*/
            // value字段值
            val values = DbExpr.question.repeateToArray(columns.size)
            // columns 顺序
            val query = reuseQueryBuilder().insertColumns(*columns.toTypedArray()).value(*values)

            // 构建参数
            val params: ArrayList<Any?> = ArrayList()
            // props 顺序 = columns 顺序
            for (item in items) {
                for (prop in props) {
                    params.add(item[prop])
                }
            }

            // 批量插入
            val result = query.batchInsert(params, db)

            // 后置
            for (item in items) {
                if (item is Orm)
                    item.triggerAfterCreate()
            }

            result
        }
    }

    /**
     * 批量更新
     *   一般用于批量更新 OrmEntity 对象, 而不是 Orm 对象, 因此也不会触发 Orm 中的前置后置事件
     *
     * @param items
     * @return
     */
    public override fun batchUpdate(items: List<IOrmEntity>): IntArray {
        if (items.isEmpty())
            return emptyIntArray()

        // 校验
        for (item in items) {
            validateOrThrow(item)
        }

        return db.transaction {
            // 前置
            for (item in items) {
                if (item is Orm)
                    item.triggerBeforeUpdate()
            }

            // 构建update语句
            val query = reuseQueryBuilder()
            // set字段: 取全部字段, 不能取第一个元素的字段, 因为每个元素可能修改的字段都不一样, 这样会导致其他元素漏掉更新某些字段
            //val props = (items.first() as OrmEntity).getDoc().keys
            val props = ArrayList(this.props)
            props.removeAll(primaryProp.columns)
            for (prop in props)
                query.set(prop2Column(prop), DbExpr.question)

            // where主键
            query.where(primaryKey, DbExpr.question)

            // 构建参数
            val params: ArrayList<Any?> = ArrayList()
            for (item in items) {
                // 属性值
                for (prop in props)
                    params.add(item[prop])
                // 主键值
                for (pk in primaryProp.columns)
                    params.add(item[pk])
            }

            // 批量更新
            val result = query.batchUpdate(params, db);

            // 后置
            for (item in items) {
                if (item is Orm)
                    item.triggerAfterUpdate()
            }

            result
        }
    }

    /**
     * 批量删除
     *
     * @param pks 主键值列表, 主键值可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    public override fun batchDelete(vararg pks: Any): IntArray {
        // 构建delete语句
        val query = reuseQueryBuilder().where(primaryKey, DbExpr.question)

        // 构建参数
        val params: ArrayList<Any?> = ArrayList()
        for (pk in pks) {
            if (pk is DbKey<*>)
                params.addAll(pk.columns)
            else
                params.add(pk)
        }

        return query.batchDelete(params, db)
    }

    /**
     * 联查关联表
     *
     * @param query 查询构建器
     * @param name 关联关系名, 类型 String|DbExpr(关系名+别名)
     * @param select 是否select关联字段
     * @param columns 关联字段列表
     * @param lastName 上一级关系名, 类型 String|DbExpr(关系名+别名)
     * @param path 列名父路径
     * @param queryAction 查询对象的回调函数, 只针对 hasMany 关系
     * @return 关联关系
     */
    public override fun joinRelated(query: OrmQueryBuilder, name: CharSequence, select: Boolean, columns: SelectColumnList?, lastName: CharSequence, path: String, queryAction: ((OrmQueryBuilder, Boolean)->Unit)?): IRelation {
        // 获得当前关联关系
        val relation = getRelation(name.toString())!! // 兼容 name 类型是 DbExpr, 用 DbExpr.toString() 来引用关系名
        // 1 hasMany关系：只处理一层
        if (relation.isHasMany) {
            // 单独处理hasMany关系，不在一个sql中联查，而是单独查询
            query.withMany(name.toString(), columns, queryAction)
            return relation;
        }

        // 2 非hasMany关系
        val alias = if (name is DbExpr) name.alias!! else name.toString() // 当前关系别名, 用作表别名
        val lastAlias = if (lastName is DbExpr) lastName.alias!! else lastName.toString() // 上一级关系别名, 用作表别名
        // join关联表
        relation.applyQueryJoinRelatedAndCondition(query, lastAlias, alias)

        //列名父路径
        val path2 = if (path == "")
                        name.toString()
                    else
                        path + ":" + name

        // 递归联查子关系
        columns?.forEachRelatedColumns { subname: CharSequence, subcolumns: SelectColumnList? ->
            relation.ormMeta.joinRelated(query, subname, select, subcolumns, name, path2)
        }

        // 查询当前关系字段
        if (select)
            query.selectRelated(relation, alias, columns?.myColumns /* 若字段为空，则查全部字段 */, path2);

        return relation;
    }

    /********************************* 关联关系 **************************************/
    /**
     * hasN / hasNThrough关联关系
     *   只有hasN / hasNThrough关联关系, 才能级联删除
     */
    public override val hasNOrThroughRelations: List<IRelation> by lazy{
        relations.values.filter { relation ->
            !relation.isBelongsTo // 过滤 hasN / hasNThrough
        }.sortedBy { relation -> // // 从小到大
            // 优先hasNThrough, 主要是用在关联删除时, 优先删除hasNThrough, 防止先删除[hasN 中间表],导致[hasNThrough 中间表]无数据可删
            var sort = if(relation is HasNThroughRelation) 0 else 10000
            // 优先条件更少的, 覆盖面更广
            sort += relation.conditions.size
            sort
        }
    }

    /**
     * 是否有某个关联关系
     * @param name
     * @return
     */
    public override fun hasRelation(name: String): Boolean {
        //return name in relations; // 坑爹啊，ConcurrentHashMap下的 in 语义是调用 contains()，但是我想调用 containsKey()
        return relations.isNotEmpty() // 优化性能
                && relations.containsKey(name)
    }

    /**
     * 添加关联关系
     * @param name
     * @param relation
     * @return
     */
    public fun addRelation(name: String, relation: IRelation): OrmMeta {
        // 检查关系的主键外键是否存在
        relation.checkKeyExist()

        // 添加关系
        relations.put(name, relation)
        (relation as Relation).name = name
        return this
    }

    /**
     * 获得某个关联关系
     * @param name
     * @return
     */
    public override fun getRelation(name: String): IRelation? {
        return relations.get(name);
    }

    /**
     * 生成属性代理 + 设置关联关系(belongs to)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    public override fun belongsTo(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, conditions: Map<String, Any?>, queryAction: ((OrmQueryBuilder, Boolean)->Unit)?): IOrmMeta {
        // 设置关联关系
        return addRelation(name, BelongsToRelation(this, relatedModel, foreignKey, primaryKey, RelationConditions(conditions, queryAction)))
    }

    /**
     * 设置关联关系(has one)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @param queryAction 查询对象的回调函数
     * @return
     */
    public override fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, conditions: Map<String, Any?>, cascadeDeleted: Boolean, queryAction: ((OrmQueryBuilder, Boolean)->Unit)?): IOrmMeta {
        // 设置关联关系
        return addRelation(name, HasNRelation(true, this, relatedModel, foreignKey, primaryKey, RelationConditions(conditions, queryAction), cascadeDeleted))
    }

    /**
     * 设置关联关系(has many)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @param queryAction 查询对象的回调函数
     * @return
     */
    public override fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, conditions: Map<String, Any?>, cascadeDeleted: Boolean, queryAction: ((OrmQueryBuilder, Boolean)->Unit)?): IOrmMeta {
        // 设置关联关系
        return addRelation(name, HasNRelation(false, this, relatedModel, foreignKey, primaryKey, RelationConditions(conditions, queryAction), cascadeDeleted))
    }

    /**
     * 设置关联关系(has one)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @param conditions 关联查询条件
     * @param queryAction 查询对象的回调函数
     * @return
     */
    public override fun hasOneThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, middleTable: String, farForeignKey: DbKeyNames, farPrimaryKey: DbKeyNames, conditions: Map<String, Any?>, queryAction: ((OrmQueryBuilder, Boolean)->Unit)?): IOrmMeta {
        // 设置关联关系
        return addRelation(name, HasNThroughRelation(true, this, relatedModel, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey, RelationConditions(conditions, queryAction)))
    }

    /**
     * 设置关联关系(has many)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param middleTable 中间表
     * @param farForeignKey 远端外键
     * @param farPrimaryKey 远端主键
     * @param conditions 关联查询条件
     * @param queryAction 查询对象的回调函数
     * @return
     */
    public override fun hasManyThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, middleTable: String, farForeignKey: DbKeyNames, farPrimaryKey: DbKeyNames, conditions: Map<String, Any?>, queryAction: ((OrmQueryBuilder, Boolean)->Unit)?): IOrmMeta {
        // 设置关联关系
        return addRelation(name, HasNThroughRelation(false, this, relatedModel, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey, RelationConditions(conditions, queryAction)))
    }

    /********************************* 通过回调动态获得对象的关联关系 **************************************/
    /**
     * 是否有某个回调的关联关系
     * @param name
     * @return
     */
    public override fun hasCbRelation(name: String): Boolean {
        //return name in relations; // 坑爹啊，ConcurrentHashMap下的 in 语义是调用 contains()，但是我想调用 containsKey()
        return cbRelations.isNotEmpty() // 优化性能
                && cbRelations.containsKey(name)
    }

    /**
     * 添加回调的关联关系
     * @param name
     * @param relation
     * @return
     */
    public fun addCbRelation(name: String, relation: ICbRelation<out IOrm, *, *>): OrmMeta {
        cbRelations.put(name, relation)
        (relation as CbRelation).name = name
        return this
    }

    /**
     * 获得某个回调的关联关系
     * @param name
     * @return
     */
    public override fun getCbRelation(name: String): ICbRelation<out IOrm, *, *>? {
        return cbRelations.get(name);
    }

    /**
     * 设置通过回调动态获得对象的关联关系(has one)
     * @param name 字段名
     * @param pkGetter 当前模型的主键的getter
     * @param fkGetter 关联对象的外键的getter
     * @param relatedSupplier 批量获取关联对象的回调
     * @return
     */
    public override fun <M:IOrm, K, R> cbHasOne(name: String, pkGetter: (M)->K, fkGetter: (R)->K, relatedSupplier:(List<K>) -> List<R>): IOrmMeta {
        // 设置关联关系
        return addCbRelation(name, CbRelation(false, pkGetter, fkGetter, relatedSupplier))
    }

    /**
     * 设置通过回调动态获得对象的关联关系(has many)
     * @param name 字段名
     * @param pkGetter 当前模型的主键的getter
     * @param fkGetter 关联对象的外键的getter
     * @param relatedSupplier 批量获取关联对象的回调
     * @return
     */
    public override fun <M:IOrm, K, R> cbHasMany(name: String, pkGetter: (M)->K, fkGetter: (R)->K, relatedSupplier:(List<K>) -> List<R>): IOrmMeta {
        // 设置关联关系
        return addCbRelation(name, CbRelation(true, pkGetter, fkGetter, relatedSupplier))
    }

    /********************************* xstream **************************************/
    /**
     * 初始化xstream, 入口
     * @param modelNameAsAlias 模型名作为别名
     * @return
     */
    public override fun initXStream(modelNameAsAlias: Boolean): XStream {
        val xstream = XStream()

        // 1 自定义转换器
        // 日期转换
        // fix bug: xstream默认不能解析 2020-10-10 21:32:15 的日期格式, 因此需要单独指定
        // 注意第二个参数acceptableFormats表示可接收的多种日期格式, 后续根据需求添加, 可参考私有属性 DateConverter.DEFAULT_ACCEPTABLE_FORMATS
        xstream.registerConverter(DateConverter("yyyy-MM-dd HH:mm:ss", null, TimeZone.getTimeZone("GMT+8")))
        //xstream.registerConverter(DateConverter("yyyy-MM-dd HH:mm:ss", arrayOf("yyyy-MM-dd", "HH:mm:ss", "yyyyMMdd", "HHmmss", "yyyyMMdd HHmmss", "yyyyMMddHHmmss"), TimeZone.getTimeZone("Asia/Shanghai")))
        // orm转换
        xstream.registerConverter(OrmConverter(xstream))

        // 2 初始化当前模型+关联模型的xstream
        initXStreamWithRelated(modelNameAsAlias, xstream)
        return xstream
    }

    /**
     * 初始化当前模型+关联模型的xstream
     *   递归调用关联模型, 需要考虑去重, 否则死循环
     *
     * @param modelNameAsAlias 模型名作为别名
     * @param xstream
     */
    protected fun initXStreamWithRelated(modelNameAsAlias: Boolean, xstream: XStream, doneModels: MutableSet<KClass<*>> = HashSet()) {
        // 0 去重
        if(doneModels.contains(model))
            return
        doneModels.add(model)

        // 1 模型名作为别名
        if (modelNameAsAlias)
            xstream.alias(name, model.java)

        // 2 当前模型的初始化
        initXStream(xstream)

        // 3 关联模型的初始化
        for ((name, relation) in relations) {
            val related = relation.ormMeta
            // 关联模型名作为别名
            if (modelNameAsAlias)
                xstream.alias(related.name, related.model.java)
            // 关联模型的初始化
            related.initXStreamWithRelated(modelNameAsAlias, xstream, doneModels)
        }
    }

    /**
     * 内部初始化当前模型的xstream, 子类实现
     */
    protected open fun initXStream(xstream: XStream) {
    }

}

