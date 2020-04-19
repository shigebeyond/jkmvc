package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * 密码框
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-20 18:57:59
 */
class PasswordInputTag : InputTag("password") {

    /**
     * 是否显示密码
     */
    var isShowPassword = false

    override fun beforeWriteTag(writer: JspWriter) {
        super.beforeWriteTag(writer)

        if(!isShowPassword)
            value = ""
    }

    override fun reset() {
        super.reset()

        isShowPassword = false
    }

}
