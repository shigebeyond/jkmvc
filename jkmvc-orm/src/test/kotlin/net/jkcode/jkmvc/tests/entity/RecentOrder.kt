package net.jkcode.jkmvc.tests.entity

import net.jkcode.jkmvc.es.annotation.EsDoc
import net.jkcode.jkmvc.es.annotation.EsId
import net.jkcode.jkmvc.orm.OrmEntity

/**
 * 最近订单
 */
@EsDoc(index = "recent_order_index")
public class RecentOrder: OrmEntity() {

    @EsId
    public var id: Long by property()

    public var cargoId: Long by property() // 货物id

    public var driverUserName: String by property() // 司机名

    public var loadAddress: String by property() //

    public var searchable: Boolean by property() // 是否可搜索

    public var companyId: Int by property() // 公司id
}