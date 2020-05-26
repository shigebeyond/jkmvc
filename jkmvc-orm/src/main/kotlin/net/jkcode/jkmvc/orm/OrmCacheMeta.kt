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
     * 是否联查某个关联属性
     * @param name
     * @return
     */
    public fun hasWithRelation(name: String): Boolean {
        return withs.contains(name)
    }

}