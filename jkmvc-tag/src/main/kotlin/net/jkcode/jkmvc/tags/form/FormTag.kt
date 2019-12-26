package net.jkcode.jkmvc.tags.form

import net.jkcode.jkmvc.http.HttpRequest
import javax.servlet.jsp.JspWriter

/**
 * 表单
 *    modelAttribute 也是代理到 path
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class FormTag: HtmlTag("from", true) {

    // 模型在请求属性名
    fun setModelAttribute(modelAttribute: String) {
        this.path = modelAttribute
    }

    fun setCommandName(commandName: String) {
        this.path = commandName
    }

    public var action: String by property()

    public var method: String by property()

    public var target: String by property()

    public var enctype: String by property()

    public var acceptCharset: String by property()

    public var onsubmit: String by property()

    public var onreset: String by property()

    public var autocomplete: String by property()

    /**
     * 输出标签前处理
     */
    protected override fun beforeWriteTag(writer: JspWriter) {
        if (!method.equals("GET", true) && !method.equals("POST", true))
            throw IllegalArgumentException("Invalid HTTP method: $method")

        if(path == null)
            throw IllegalArgumentException("modelAttribute(path) must not be null")

        if(action == null)
            throw IllegalArgumentException("action must not be null")
        action = HttpRequest.current().absoluteUrl(action)
    }

}
