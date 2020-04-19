package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * 输入框
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-20 18:57:59
 */
open class InputTag(type: String = "text" /* 类型, 子类可改写 */) : BaseInputTag("input", false) {

    public var type: String by property()

    init {
        this.type = type
    }

    public var size: String? by property()

    public var maxlength: String? by property()

    public var alt: String? by property()

    public var onselect: String? by property()

    public var autocomplete: String? by property()

    override fun beforeWriteTag(writer: JspWriter) {
        // value 最好要输出的, 因为前端经常会访问该属性
        if(value == null)
            value = boundValue ?: ""
    }

}
