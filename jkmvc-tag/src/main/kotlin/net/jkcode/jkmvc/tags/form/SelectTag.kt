package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * 下拉框
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class SelectTag: BaseInputTag("select", true){

    // 可见选项的数目
    public var size: String? by property()

    // 是否多选
    public var multiple: String? by property()

    override fun beforeWriteTag(writer: JspWriter) {
        // 根据绑定值类型是否
        if (multiple == null && boundType != null && typeRequiresMultiple(boundType!!))
            multiple = "true"
    }

    /**
     * Returns '`true`' for arrays, [Collections][Collection]
     * and [Maps][Map].
     */
    protected fun typeRequiresMultiple(type: Class<*>): Boolean {
        return type.isArray || Collection::class.java.isAssignableFrom(type) || Map::class.java.isAssignableFrom(type)
    }
}
