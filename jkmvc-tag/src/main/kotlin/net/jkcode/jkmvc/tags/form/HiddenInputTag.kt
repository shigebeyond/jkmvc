package net.jkcode.jkmvc.tags.form

/**
 * 隐藏域
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class HiddenInputTag: HtmlTag("input", false) {

    init{
        attrs["type"] = "hidden"
    }

    public var value: Any? by property()

}
