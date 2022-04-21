package net.jkcode.jkmvc.http.jphp

import php.runtime.env.CompileScope
import php.runtime.ext.support.Extension
import org.develnext.jphp.zend.ext.ZendExtension

class JkmvcHttpExtension : Extension() {

    companion object {
        const val NS = "php\\jkmvc\\http"
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
        return arrayOf("jkmvc\\http")
    }

    override fun onRegister(scope: CompileScope) {
        registerClass(scope, PHttpRequest::class.java)
        registerClass(scope, PHttpResponse::class.java)
    }

}