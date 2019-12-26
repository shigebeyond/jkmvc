package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * 单选框
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:08 AM
 */
class CheckboxTag : BaseSingleCheckedTag("checkbox") {

    override fun beforeWriteTag(writer: JspWriter) {
        if(boundValue is Boolean && boundValue as Boolean || boundValue is String && java.lang.Boolean.valueOf(boundValue as String)){
            attrs["checked"] = true
            return
        }

        if (value == null)
            throw IllegalArgumentException("Attribute 'value' is required when binding to non-boolean values")
    }


}
