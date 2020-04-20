package net.jkcode.jkmvc.tags.form

import javax.servlet.jsp.JspWriter

/**
 * Form tag for displaying errors for a particular field or object.
 * This tag supports three main usage patterns:
 *  1. Field only - set '`path`' to the field name (or path)
 *  1. Object errors only - omit '`path`'
 *  1. All errors - set '`path`' to '`*`'
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class ErrorsTag : HtmlTag(null, true){

    companion object{

        //　默认容器
        public val DEFAULT_ELEMENT = "span"

        // 默认分隔符
        public val DEFAULT_DELIMITER = "<br/>"
    }

    /**
     * 容器
     */
    public var element: String? = null

    /**
     * 分隔符
     */
    public var delimiter: String? = null

    override fun writeBody(writer: JspWriter) {
        // 无错不输出
        if(!isError)
            return

        val element = this.element ?: DEFAULT_ELEMENT
        val delimiter = this.delimiter ?: DEFAULT_DELIMITER

        // 容器头
        writer.append("<").append(element).append(">")

        // 尝试获得多个错误
        var errors: Collection<*>? = null
        if(boundError is Map<*, *>)
            errors = (boundError as Map<*, *>).values
        else if(boundError is Collection<*>)
            errors = boundError as Collection<*>

        if(errors != null){ // 多个错误
            var i = 0
            errors.forEach { err ->
                if(i++ > 0)
                    writer.append(delimiter)
                writer.append(toDisplayString(err))
            }
        }else{ // 单个错误
            writer.append(toDisplayString(boundError))
        }

        // 容器尾部
        writer.append("</").append(element).append(">")
    }

    override fun reset() {
        super.reset()

        element = null
        delimiter = null
    }

}
