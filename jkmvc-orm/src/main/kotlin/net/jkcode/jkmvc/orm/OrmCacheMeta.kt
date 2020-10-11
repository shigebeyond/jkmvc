package net.jkcode.jkmvc.orm

/**
 * orm缓存的元数据
 *
 * @author shijianhang
 * @date 2020-3-10 上午12:52:34
 */
class OrmCacheMeta(
        public val cacheType: String= "lru", // 缓存类型, 如 lru/jedis
        public val withs: Array<String> = emptyArray(), // 联查对象属性
        public val initAll: Boolean = false // 一开始就缓存全部数据
) {

    /**
     * 模型元数据
     */
    public lateinit var ormMeta:IOrmMeta

    /**
     * 是否联查某个关联属性
     * @param name
     * @return
     */
    public fun hasWithRelation(name: String): Boolean {
        return withs.contains(name)
    }

    /**
     * 是否联查某个关联属性
     * @param relatedMeta 关联模型的元数据
     * @return
     */
    public fun hasWithRelation(relatedMeta: OrmMeta): Boolean {
        // 遍历每个关联关系, 匹配关联模型的元数据
        for((name, relation) in ormMeta.relations){
            // 匹配关联模型的元数据 + 连带缓存了该关联对象
            if(relation.ormMeta == relatedMeta && hasWithRelation(name))
                return true
        }
        return false
    }

    /**
     * 对query builder应用联查
     * @param query
     */
    public fun applyQueryWiths(query: OrmQueryBuilder) {
        if(withs.isEmpty())
            return

        // fix bug: 不能联查下一级的
        //query.withs(*withs)
        query.selectWiths(*build2LevelWiths().toTypedArray())
    }

    /**
     * 构建2级的联查对象属性, 其结果用在 OrmQueryBuilder.selectWiths(...)
     *   TODO: 支持多级联查
     */
    protected fun build2LevelWiths(): List<Any> {
        return withs.map { name ->
            val relation = ormMeta.getRelation(name)!!
            val nextOrmMeta = relation.ormMeta
            val nextCacheMeta = nextOrmMeta.cacheMeta
            if(nextCacheMeta?.withs.isNullOrEmpty()){ // 无下一级联查
                name
            }else{ // 有下一级联查
                // name to nextCacheMeta!!.withs.toList() // 如果name是一对多的关系,会自动select *, 否则不会, 因此需要指定name对应模型的字段
                // name to nextOrmMeta.props + nextCacheMeta!!.withs
                name to listOf("*") + nextCacheMeta!!.withs // 简写
            }
        }
    }
}