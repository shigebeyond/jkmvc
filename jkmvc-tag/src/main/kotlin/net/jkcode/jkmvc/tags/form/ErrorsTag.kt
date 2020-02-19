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

    /**
     * 容器
     */
    public var element = "span"

    /**
     * 分隔符
     */
    public var delimiter = "<br/>"

    override fun writeBody(writer: JspWriter) {
        // 无错不输出
        if(!isError)
            return

        // 容器头
        writer.append("<").append(element).append(">")

        val error = boundError
        if(error is Map<*, *>){ // 多个错误
            var i = 0
            error.forEach { field, err ->
                if(i++ > 0)
                    writer.append(delimiter)
                writer.append(toDisplayString(err))
            }
        }else{ // 单个错误
            writer.append(toDisplayString(error))
        }


        // 容器尾部
        writer.append("</").append(element).append(">")

    }


}
