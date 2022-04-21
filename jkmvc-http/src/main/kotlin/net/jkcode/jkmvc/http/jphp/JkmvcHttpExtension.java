package net.jkcode.jkmvc.http.jphp;

import php.runtime.env.CompileScope;
import php.runtime.ext.support.Extension;

public class JkmvcHttpExtension extends Extension {
    public static final String NS = "php\\jkmvc\\http";

    @Override
    public Status getStatus() {
        return Status.EXPERIMENTAL;
    }

    @Override
    public String[] getPackageNames() {
        return new String[] { "jkmvc\\http" };
    }

    @Override
    public void onRegister(CompileScope scope) {
        registerClass(scope, PHttpRequest.class);
        registerClass(scope, PHttpResponse.class);
    }
}
