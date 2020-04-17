package net.jkcode.jkmvc.tags.form

import net.jkcode.jkutil.common.getPropertyValue
import net.jkcode.jkutil.message.MessageSource
import org.apache.commons.lang.StringEscapeUtils
import java.io.IOException
import javax.servlet.jsp.JspTagException
import javax.servlet.jsp.tagext.Tag

/**
 * 改进 jstl 中 <fmt:message>, 让他能够从多个bundle文件加载消息
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class MessageTag : org.apache.taglibs.standard.tag.rt.fmt.MessageTag() {

    companion object {

        /**
         * Default separator for splitting an arguments String: a comma (",")
         */
        val DEFAULT_ARGUMENT_SEPARATOR = ","
    }

    /**
     * 获得父类私有属性var
     */
    protected val `var`: String?
        get() = this.getPropertyValue("var") as String?

    /**
     * 获得父类私有属性scope
     */
    protected val scope: Int
        get() = this.getPropertyValue("scope") as Int

    /**
     * Resolves the message, escapes it if demanded,
     * and writes it to the page (or exposes it as variable).
     */
    override fun doEndTag(): Int {
        // 获得key: key属性 / 标签内容
        var key: String? = null
        if (this.keySpecified) {
            key = this.keyAttrValue
        } else if (this.bodyContent?.string != null) {
            key = this.bodyContent.string.trim()
        }

        // key为空, 直接输出?
        if (key.isNullOrBlank()) {
            writeMessage("??????")
            return Tag.EVAL_PAGE
        }

        // key不为空, 则取消息
        var msg = MessageSource.instance().getMessage(key)
        msg = StringEscapeUtils.escapeHtml(msg)

        // Expose as variable, if demanded, else write to the page.
        if (this.`var` != null) {
            pageContext.setAttribute(this.`var`, msg, this.scope)
        } else {
            writeMessage(msg)
        }

        return Tag.EVAL_PAGE
    }

    protected fun writeMessage(msg: String) {
        try {
            pageContext.out.print(msg)
        } catch (ioe: IOException) {
            throw JspTagException(ioe.toString(), ioe)
        }
    }

}
