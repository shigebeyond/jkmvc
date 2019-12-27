package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.common.iteratorArrayOrCollection
import javax.servlet.jsp.JspWriter

/**
 * 单选控件基类
 *   type=checkbox/radio
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:08 AM
 */
open class BaseSingleCheckedTag(type: String = "") : InputTag(type) {

    companion object {

        val labelTags = ThreadLocal.withInitial {
            LabelTag()
        }
    }

    public var label: String? by property()

    public var checked: Boolean? by property()

    override fun beforeWriteTag(writer: JspWriter) {
        // 选中
        if(checked == null && isChecked(value))
            checked = true

        // 必须在 isChecked(value) 后给 value 赋值
        if(value == null && boundValue != null)
            value = boundValue
    }

    /**
     * 是否选中
     * @return
     */
    protected open fun isChecked(vaule: Any?): Boolean {
        // null
        if(boundValue == null || value == null)
            return false

        // bool
        if(boundValue is Boolean)
            return boundValue is String && java.lang.Boolean.valueOf(boundValue as String)

        // map, 仅针对checkbox
        if (boundValue is Map<*, *>)
            return (boundValue as Map<*, *>).containsKey(value)

        // 数组/集合, 仅针对checkbox
        if (boundValue!!.javaClass.isArray || boundValue is Collection<*>) {
            val it = boundValue!!.iteratorArrayOrCollection()!!
            for(v in it)
                if(v == value)
                    return true

            return false
        }

        return false
    }

    override fun afterWriteTag(writer: JspWriter) {
        val tag = labelTags.get()
        tag.clear()
        tag.`for` = id
        tag.writeTag(writer)
    }

}
