package net.jkcode.jkmvc.http.jphp

import net.jkcode.jkmvc.http.HttpRequest
import net.jkcode.jkmvc.http.HttpResponse
import net.jkcode.jphp.ext.JphpLauncher

class JphpHttpRequestHandler{

    fun callController(req: HttpRequest, res: HttpResponse) {
        val lan = JphpLauncher.instance()
        val file = req.controller + ".php"
        val data = mapOf(
                "req" to PHttpRequest(lan.environment, req),
                "res" to PHttpResponse(lan.environment, res)
        )
        lan.run(file, data)
    }
}