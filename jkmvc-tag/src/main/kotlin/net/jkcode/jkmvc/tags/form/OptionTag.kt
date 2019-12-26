package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * 单个选项
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class OptionTag: HtmlTag("option", true) {

    public var selected: Boolean by property()

    public var value: Any? by property()

    public fun setLabel(label: String){
        body = label
    }

    /**
     * 父select标签
     */
    public val selectTag: SelectTag
        get() {
            return findAncestorWithClass(this, SelectTag::class.java) as SelectTag
        }

    override fun beforeWriteTag(writer: JspWriter) {
        // 等于父组件的值即选中
        selected = selectTag.isBoundValueEquals(value)
    }

}