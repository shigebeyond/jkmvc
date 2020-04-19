package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.ttl.AllRequestScopedTransferableThreadLocal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * id生成器
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:08 AM
 */
public enum class IdGenerator {
    // name即id
    ByName{
        override fun nextId(tag: HtmlTag): String? {
            return tag.name?.replace(nameNouseRegex, "")
        }
    },
    // 自增id
    ByAutoIncr{
        override fun nextId(tag: HtmlTag): String? {
            val tag = tag::class.simpleName
            return tag + idGenerators.get().getOrPut(tag){
                AtomicLong(0)
            }.incrementAndGet()
        }
    },
    // 无id
    No;

    /**
     * 生成id
     */
    public open fun nextId(tag: HtmlTag): String?{
        return null
    }

    companion object{

        /**
         * name中无用字符
         */
        protected val nameNouseRegex = "\\[\\]".toRegex()

        /**
         * id生成器
         */
        protected val idGenerators: AllRequestScopedTransferableThreadLocal<ConcurrentHashMap<String, AtomicLong>> = AllRequestScopedTransferableThreadLocal {
            ConcurrentHashMap<String, AtomicLong>()
        }
    }

}