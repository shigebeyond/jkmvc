package net.jkcode.jkmvc.tags.form

/**
 * 文本域
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-24 10:43 AM
 */
class TextareaTag: BaseInputTag("textarea", true){

    public var rows: String? by property()

    public var cols: String? by property()

    public var onselect: String? by property()

}
