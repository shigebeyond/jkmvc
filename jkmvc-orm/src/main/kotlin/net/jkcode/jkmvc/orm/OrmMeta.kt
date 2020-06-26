package net.jkcode.jkmvc.orm

import com.thoughtworks.xstream.XStream
import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.db.IDb
import net.jkcode.jkmvc.model.GeneralModel
import net.jkcode.jkmvc.query.DbExpr
import net.jkcode.jkutil.cache.ICache
import net.jkcode.jkutil.collection.FixedKeyMapFactory
import net.jkcode.jkutil.common.*
import net.jkcode.jkutil.validator.IValidator
import net.jkcode.jkutil.validator.ModelValidateResult
import net.jkcode.jkutil.validator.ValidateResult
import java.lang.reflect.Constructor
import java.util.*
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
                   public override val pkEmptyRule: PkEmptyRule = PkEmptyRule.default // 检查主键为空的规则
) : IOrmMeta {

    public constructor(
            model: KClass<out IOrm>, // 模型类
            label: String, // 模型中文名
            table: String, // 表名，假定model类名, 都是以"Model"作为后缀
            primaryKey: String, // 主键
            cacheMeta: OrmCacheMeta? = null, // 缓存配置
            dbName: String = "default", // 数据库名
            pkEmptyRule: PkEmptyRule = PkEmptyRule.default // 检查主键为空的规则
    ) : this(model, label, table, DbKeyNames(primaryKey), cacheMeta, dbName, pkEmptyRule)

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
        // 检查 model 类的默认构造函数
        if (model != GeneralModel::class) {
            constructorNoarg = model.java.getConstructorOrNull() // 无参数构造函数
            constructorVararg = model.java.getConstructorOrNull(Array<Any>::class.java) // 可变参数构造函数
            if (constructorNoarg == null && constructorVararg == null)
                throw OrmException("Model Class [$model] has no no-arg constructor") // Model类${clazz}无默认构造函数
        }

        // 一开始缓存全部
        if (cacheMeta != null && cacheMeta!!.initAll) {
            val query = queryBuilder()
            // 缓存时联查
            if(cacheMeta!!.withs.isNotEmpty())
                query.withs(*cacheMeta!!.withs)
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
    public override val relations: MutableMap<String, IRelationMeta> by lazy {
        HashMap<String, IRelationMeta>()
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
            val method = model.java.getMethod(event)
            method.declaringClass != OrmEntity::class.java // 实际上事件处理方法是定义在 IOrmPersistent 接口, 但反射中获得声明类却是 OrmEntity 抽象类
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
     * 表字段
     */
    public override val columns: Collection<String> by lazy {
        db.getColumnsByTable(table).map { it.name }
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
     * 对象属性名+关系名
     */
    public override val propsAndRelations: List<String> by lazy {
        props + relations.keys
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
                if (relation.type == RelationType.BELONGS_TO) { // 只对belongsTo有效, 表示本模型有外键
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
                if (relation is MiddleRelationMeta)
                    keys.addAll(relation.middleForeignProp.columns)
            }
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
            throw OrmException("类 ${model} 没有属性: $column");

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
        val needTrans = canHandleAnyEvent(events) || withHasRelations
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
    public override fun validate(item: IOrmEntity): ModelValidateResult {
        // 逐个属性校验
        val errors = HashMap<String, String>()
        for ((field, rule) in rules) {
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
        if (cacheMeta == null)
            return

        val key = getCacheKey(item.pk)
        cache.remove(key)
    }

    /**
     * 读缓存, 无则读db
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     * @param expires 缓存过期时间
     * @return
     */
    public override fun <T : IOrm> getOrPutCache(pk: DbKeyValues, item: T?, expires: Long): T? {
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
     * 根据主键值来加载数据
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     */
    protected fun <T : IOrm> innerloadByPk(pk: DbKeyValues, item: T? = null): T? {
        if (isPkEmpty(pk))
            return null

        val query = queryBuilder()
        // 缓存时联查
        if (cacheMeta != null && cacheMeta!!.withs.isNotEmpty()) {
            query.withs(*cacheMeta!!.withs)
        }
        // 查询主键
        return query.where(primaryKey, pk).findRow {
            //val result = item ?: model.java.newInstance() as T
            val result = item ?: newInstance() as T
            result.setOriginal(it)
            result
        }
    }

    /**
     * 根据主键值来加载数据
     * @param pk 要查询的主键
     * @param item 要赋值的对象
     */
    public override fun loadByPk(pk: DbKeyValues, item: IOrm) {
        //innerloadByPk(pk, item)
        getOrPutCache(pk, item)
    }

    /**
     * 根据主键值来查找数据
     * @param pk 要查询的主键
     * @return
     */
    public override fun <T : IOrm> findByPk(pk: DbKeyValues): T? {
        //return innerloadByPk(pk)
        return getOrPutCache(pk)
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

        val item = findByPk<IOrm>(pk)
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
            validate(item)
        }

        return db.transaction {
            // 前置
            for (item in items) {
                if (item is Orm)
                    item.triggerBeforeCreate()
            }

            // 构建insert语句
            // insert字段 -- 取全部字段, 不能取第一个元素的字段, 因为每个元素可能修改的字段都不一样, 这样会导致其他元素漏掉更新某些字段
            /*val props = (items.first() as OrmEntity).getData().keys
            val columns = props.mapToArray { prop ->
                prop2Column(prop)
            }*/
            // value字段值
            val values = DbExpr.question.repeateToArray(columns.size)
            // columns 顺序
            val query = queryBuilder().insertColumns(*columns.toTypedArray()).value(*values)

            // 构建参数
            val params: ArrayList<Any?> = ArrayList()
            // props 顺序 = columns 顺序
            for (item in items) {
                for (prop in props) {
                    params.add(item[prop])
                }
            }

            // 批量插入
            val result = query.batchInsert(params)

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
            validate(item)
        }

        return db.transaction {
            // 前置
            for (item in items) {
                if (item is Orm)
                    item.triggerBeforeUpdate()
            }

            // 构建update语句
            val query = queryBuilder()
            // set字段: 取全部字段, 不能取第一个元素的字段, 因为每个元素可能修改的字段都不一样, 这样会导致其他元素漏掉更新某些字段
            //val props = (items.first() as OrmEntity).getData().keys
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
            val result = query.batchUpdate(params);

            // 后置
            for (item in items) {
                if (item is Orm)
                    item.triggerAfterUpdate()
            }

            result
        }
    }

    /**
     * 单个删除
     *
     * @param pk 主键值, 可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    public override fun delete(pk: Any): Boolean {
        // 构建delete语句
        val query = queryBuilder().where(primaryKey, DbExpr.question)

        // 构建参数
        val params: ArrayList<Any?> = ArrayList()
        if (pk is DbKey<*>)
            params.addAll(pk.columns)
        else
            params.add(pk)

        return query.delete(params)
    }

    /**
     * 批量删除
     *
     * @param pks 主键值列表, 主键值可能是单主键(Any), 也可能是多主键(DbKey)
     * @return
     */
    public override fun batchDelete(vararg pks: Any): IntArray {
        // 构建delete语句
        val query = queryBuilder().where(primaryKey, DbExpr.question)

        // 构建参数
        val params: ArrayList<Any?> = ArrayList()
        for (pk in pks) {
            if (pk is DbKey<*>)
                params.addAll(pk.columns)
            else
                params.add(pk)
        }

        return query.batchDelete(params)
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
    public override fun joinRelated(query: OrmQueryBuilder, name: CharSequence, select: Boolean, columns: SelectColumnList?, lastName: CharSequence, path: String, queryAction: ((OrmQueryBuilder)->Unit)?): IRelationMeta {
        // 获得当前关联关系
        val relation = getRelation(name.toString())!! // 兼容 name 类型是 DbExpr, 用 DbExpr.toString() 来引用关系名
        // 1 非hasMany关系：只处理一层
        if (relation.type == RelationType.HAS_MANY) {
            // 单独处理hasMany关系，不在一个sql中联查，而是单独查询
            query.withMany(name.toString(), columns, queryAction)
            return relation;
        }

        // 2 非hasMany关系
        val alias = if (name is DbExpr) name.alias!! else name.toString() // 当前关系别名, 用作表别名
        val lastAlias = if (lastName is DbExpr) lastName.alias!! else lastName.toString() // 上一级关系别名, 用作表别名
        // join关联表
        if (relation.type == RelationType.BELONGS_TO) { // belongsto: join 主表
            query.joinMaster(this, lastAlias, relation, alias);
        } else { // hasxxx: join 从表
            if (relation is MiddleRelationMeta) // 有中间表
                query.joinSlaveThrough(this, lastAlias, relation, alias);
            else // 无中间表
                query.joinSlave(this, lastAlias, relation, alias);
        }

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
     * 是否有某个关联关系
     * @param name
     * @return
     */
    public override fun hasRelation(name: String): Boolean {
        //return name in relations; // 坑爹啊，ConcurrentHashMap下的 in 语义是调用 contains()，但是我想调用 containsKey()
        return relations.containsKey(name)
    }

    /**
     * 获得某个关联关系
     * @param name
     * @return
     */
    public override fun getRelation(name: String): IRelationMeta? {
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
    public override fun belongsTo(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, conditions: Map<String, Any?>): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            RelationMeta(this, RelationType.BELONGS_TO, relatedModel, foreignKey, primaryKey, conditions)
        }

        return this;
    }

    /**
     * 设置关联关系(has one)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    public override fun hasOne(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, conditions: Map<String, Any?>, cascadeDeleted: Boolean): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            RelationMeta(this, RelationType.HAS_ONE, relatedModel, foreignKey, primaryKey, conditions, cascadeDeleted)
        }

        return this;
    }

    /**
     * 设置关联关系(has many)
     * @param name 字段名
     * @param relatedModel 关联模型
     * @param foreignKey 外键
     * @param primaryKey 主键
     * @param conditions 关联查询条件
     * @param cascadeDeleted 是否级联删除
     * @return
     */
    public override fun hasMany(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, conditions: Map<String, Any?>, cascadeDeleted: Boolean): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            RelationMeta(this, RelationType.HAS_MANY, relatedModel, foreignKey, primaryKey, conditions, cascadeDeleted)
        }

        return this;
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
     * @return
     */
    public override fun hasOneThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, middleTable: String, farForeignKey: DbKeyNames, farPrimaryKey: DbKeyNames, conditions: Map<String, Any?>): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            MiddleRelationMeta(this, RelationType.HAS_ONE, relatedModel, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey, conditions)
        }

        return this;
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
     * @return
     */
    public override fun hasManyThrough(name: String, relatedModel: KClass<out IOrm>, foreignKey: DbKeyNames, primaryKey: DbKeyNames, middleTable: String, farForeignKey: DbKeyNames, farPrimaryKey: DbKeyNames, conditions: Map<String, Any?>): IOrmMeta {
        // 设置关联关系
        relations.getOrPut(name) {
            MiddleRelationMeta(this, RelationType.HAS_MANY, relatedModel, foreignKey, primaryKey, middleTable, farForeignKey, farPrimaryKey, conditions)
        }

        return this;
    }

    /********************************* xstream **************************************/
    /**
     * 初始化xstream, 入口
     * @param modelNameAsAlias 模型名作为别名
     * @return
     */
    public override fun initXStream(modelNameAsAlias: Boolean): XStream {
        val xstream = XStream()
        // 1 模型名作为别名
        if (modelNameAsAlias)
            xstream.alias(name, model.java)
        // 2 orm的转换器
        xstream.registerConverter(OrmConverter(xstream))
        // 3 当前模型的初始化
        initXStream(xstream)
        // 4 关联模型的初始化
        for ((name, relation) in relations) {
            val related = relation.ormMeta as OrmMeta
            // 关联模型名作为别名
            if (modelNameAsAlias)
                xstream.alias(related.name, related.model.java)
            // 关联模型的初始化
            related.initXStream(xstream)
        }
        return xstream
    }

    /**
     * 内部初始化xstream, 子类实现
     */
    protected open fun initXStream(xstream: XStream) {
    }

}

