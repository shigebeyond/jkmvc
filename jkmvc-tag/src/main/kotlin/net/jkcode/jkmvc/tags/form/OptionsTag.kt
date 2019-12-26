package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * 多个选项
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
open class OptionsTag: ItemsTag(null){

    companion object {
        val optionTags = ThreadLocal.withInitial {
            OptionTag()
        }
    }

    /**
     * 父select标签
     */
    public val selectTag: SelectTag
        get() {
            return findAncestorWithClass(this, SelectTag::class.java) as SelectTag
        }

    /**
     * 输出单个选项
     */
    protected override fun renderItem(writer: JspWriter, value: Any?, label: Any?, i: Int) {
        val tag = optionTags.get()
        tag.clear()
        tag.value = value
        tag.setLabel(toDisplayString(label))
        // 等于父组件的值即选中
        if(selectTag.isBoundValueEquals(value))
          tag.selected = true
        tag.writeTag(writer)
    }
}
