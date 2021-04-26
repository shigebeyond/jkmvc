package net.jkcode.jkmvc.tests.es

import net.jkcode.jkmvc.es.EsId
import net.jkcode.jkmvc.orm.OrmEntity

public class RecentOrder: OrmEntity() {

    @EsId
    public var id: Long by property()

    public var cargoId: Long by property()

    public var driverUserName: String by property()

    public var loadAddress: String by property()

    public var searchable: Boolean by property()

    public var companyId: Int by property()
}