package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * 文本域
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class TextareaTag: BaseInputTag("textarea", true){

    public var rows: String? by property()

    public var cols: String? by property()

    public var onselect: String? by property()

    override fun beforeWriteTag(writer: JspWriter) {
        if(value == null && boundValue != null)
            value = boundValue
    }

}
