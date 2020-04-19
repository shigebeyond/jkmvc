package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.common.PropertyUtil
import net.jkcode.jkutil.common.iteratorArrayOrCollection
import javax.servlet.jsp.JspException
import javax.servlet.jsp.JspWriter

/**
 * 多项目标签
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
abstract class ItemsTag(
        tag: String? , // 标签名
        protected var requireItems: Boolean = true // items是否必须
): HtmlTag(tag, true) {

    /**
     * 数据项
     */
    public var items: Any? = null

    /**
     * 值字段
     */
    public var itemValue: String? = null

    /**
     * 标签字段
     */
    public var itemLabel: String? = null

    /**
     * 输出所有选项
     */
    override fun writeBody(writer: JspWriter) {
        if (items == null) {
            if (requireItems) // 必须
                throw IllegalArgumentException("Attribute 'items' is required and must be a Collection, an Array or a Map")

            return
        }

        if (items is Map<*, *>) {
            var i = 0
            for ((key, value) in items as Map<Any, Any?>) {
                val renderValue = if (itemValue != null) PropertyUtil.get(key, itemValue!!) else key
                val renderLabel = if (itemLabel != null) PropertyUtil.get(value, itemLabel!!) else value
                renderItem(writer, renderValue, renderLabel, i++)
            }
            return
        }

        if (items!!.javaClass.isArray || items is Collection<*>) {
            var i = 0
            val it = items!!.iteratorArrayOrCollection()!!
            for (item in it) {
                val value = if (itemValue != null) PropertyUtil.get(item, itemValue!!) else item
                val label = if (itemLabel != null) PropertyUtil.get(item, itemLabel!!) else item
                renderItem(writer, value, label, i++)
            }
            return
        }

        throw JspException("Type [" + items!!.javaClass.name + "] is not valid for option items")
    }


    /**
     * 输出单个项目
     */
    protected abstract fun renderItem(writer: JspWriter, value: Any?, label: Any?, i: Int)

    override fun reset() {
        super.reset()

        items = null
        itemValue = null
        itemLabel = null
    }
}