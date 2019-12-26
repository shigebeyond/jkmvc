package net.jkcode.jkmvc.tags.form

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

    override fun afterWriteTag(writer: JspWriter) {
        val tag = labelTags.get()
        tag.clear()
        tag.`for` = id
        tag.writeTag(writer)
    }

}
