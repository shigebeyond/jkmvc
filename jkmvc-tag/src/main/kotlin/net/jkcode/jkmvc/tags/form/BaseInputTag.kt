package net.jkcode.jkmvc.tags.form

/**
 * 输入控件基类
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:08 AM
 */
abstract class BaseInputTag(
        tag: String, // 标签名
        hasBody: Boolean // 是否有标签体
): HtmlTag(tag, hasBody, IdGenerator.ByName) {

    public var onfocus: String? by property()

    public var onblur: String? by property()

    public var onchange: String? by property()

    public var accesskey: String? by property()

    public var readonly: Boolean by property()

    // 对 <select/> 无效
    public var value: Any? by property()

}