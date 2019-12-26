package net.jkcode.jkmvc.tags.form

/**
 * 标签
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class LabelTag: HtmlTag("option", true) {

    public var `for`: String? by property()

}
