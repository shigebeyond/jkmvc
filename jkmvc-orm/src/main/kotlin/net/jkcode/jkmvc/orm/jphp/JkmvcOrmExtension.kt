package net.jkcode.jkmvc.orm.jphp

import net.jkcode.jkmvc.db.Db
import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkmvc.query.DbQueryBuilder
import php.runtime.env.CompileScope
import php.runtime.ext.support.Extension
import org.develnext.jphp.zend.ext.ZendExtension

class JkmvcOrmExtension : Extension() {

    companion object {
        const val NS = "php\\jkmvc\\orm"
    }

    override fun getStatus(): Status {
        return Status.EXPERIMENTAL
    }

    override fun getRequiredExtensions(): Array<String?>? {
        return arrayOf(
                ZendExtension::class.java.getName()
        )
    }

    override fun getPackageNames(): Array<String> {
        return arrayOf("jkmvc\\orm")
    }

    override fun onRegister(scope: CompileScope) {
        registerWrapperClass(scope, Db::class.java, PDb::class.java)
        registerWrapperClass(scope, Orm::class.java, PModel::class.java)
        registerWrapperClass(scope, DbQueryBuilder::class.java, PQueryBuilder::class.java)
    }

}