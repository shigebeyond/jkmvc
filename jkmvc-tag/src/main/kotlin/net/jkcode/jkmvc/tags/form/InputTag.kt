package net.jkcode.jkmvc.tags.form

/**
 * 输入框
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-12-20 18:57:59
 */
open class InputTag(type: String = "text" /* 类型, 子类可改写 */) : BaseInputTag("input", false) {

    public var type: String by property()

    init {
        this.type = type
    }

    public var value: Any? by property()

    public var size: String? by property()

    public var maxlength: String? by property()

    public var alt: String? by property()

    public var onselect: String? by property()

    public var autocomplete: String? by property()

}
