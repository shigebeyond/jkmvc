package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.any
import net.jkcode.jkutil.common.decorateCollection
import net.jkcode.jkutil.common.decorateSet
import org.apache.commons.collections.keyvalue.DefaultMapEntry

/**
 * http参数哈希
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-11-20 21:20:33
 */
class HttpParamMap(public val params: Map<String, Array<String>>): Map<String, String?> by params as Map<String, String?>{

    /**
     * 是否包含值
     */
    public override fun containsValue(value: String?): Boolean {
        return params.any{ k, v ->
            v.contains(value)
        }
    }

    /**
     * 获得值
     */
    public override fun get(key: String): String? {
        return params.get(key)?.first()
    }

    /**
     * 获得值的集合
     */
    public override val values: Collection<String?>
        get() = decorateCollection(params.values){ v ->
            v.first()
        }

    /**
     * 获得实体的集合
     */
    public override val entries: Set<Map.Entry<String, String?>>
        get() = decorateSet(params.entries){ e ->
            DefaultMapEntry(e.key, e.value.first()) as Map.Entry<String, String?>
        }


}
