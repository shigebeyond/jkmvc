# 概述
jkmvc 整合jphp技术, 支持同构异语言(java/php), 以便支持更多的动态性, 如用php来写controller

# 实现

- 1 php代码的目录结构
参考 [demo代码](https://github.com/shigebeyond/jkmvc/tree/master/jkmvc-http/src/main/resources/jphp)
```
jkmvc/jkmvc-http/src/main/resources/jphp
├── callController.php // 内嵌的小型php mvc框架
├── controller // 控制器目录
│   └── Test.php // demo控制器
└── views // 视图目录
    └── login.php // demo视图
```

- 2 控制器
参考 [demo控制器](https://github.com/shigebeyond/jkmvc/blob/master/jkmvc-http/src/main/resources/jphp/controller/Test.php)

```
<?php
use php\jkmvc\http\IController;
use php\jkmvc\http\HttpRequest;

class Test extends IController{

    /**
     * http://localhost:8080/jkmvc-example/$test/index/1?name=shi
     * 输出
     * controller#action: test-index
     * uri: GET-/jkmvc-example/$test/index/1
     * routeUri: GET-$test/index/1
     * param: shi
     * routeParam: 1
     * query: name=shi
     * sessionId: 1l3fm2bwtu3q7yfgi35wk1svl
     */
    function index(){
//         $req = $this->req;
        $req = HttpRequest::current();
        echo 'controller#action: '. $req->controller() . '-' . $req->action() . "\n";
        echo 'uri: ' . $req->method() . '-' . $req->uri() . "\n";
        echo 'routeUri: ' . $req->method() . '-' . $req->routeUri() . "\n";
        echo 'param: ' . $req->param('name') . "\n";
        echo 'routeParam: ' . $req->param('id') . "\n";
        echo 'query: ' . $req->query() . "\n";
        echo 'sessionId: ' . $req->sessionId() . "\n";
    }

    /**
     * http://localhost:8080/jkmvc-example/$test/login
     */
    function login(){
        $this->view('login', ['msg' => '请输入登录账户和密码']);
    }
}
```

- 3 视图
参考 [demo视图](https://github.com/shigebeyond/jkmvc/blob/master/jkmvc-http/src/main/resources/jphp/views/login.php)
```
<html>
    <head>
        <meta charset="utf-8">
    </head>
    <body>
        <div class="main">
            <div class="title">
                <span>密码登录</span>
            </div>
            <div class="title-msg">
                <span><?php echo $msg;?></span>
            </div>
            <form class="login-form" method="post" novalidate >
                <!--输入框-->
                <div class="input-content">
                    <!--autoFocus-->
                    <div>
                        <input type="text" autocomplete="off"
                               placeholder="用户名" name="userNameOrEmailAddress" required/>
                    </div>
                    <div style="margin-top: 16px">
                        <input type="password"
                               autocomplete="off" placeholder="登录密码" name="password" required maxlength="32"/>
                    </div>
                </div>
                <!--登入按钮-->
                <div style="text-align: center">
                    <button type="submit" class="enter-btn" >登录</button>
                </div>

                <div class="foor">
                    <div class="left"><span>忘记密码 ?</span></div>

                    <div class="right"><span>注册账户</span></div>
                </div>
            </form>
        </div>
    </body>
<html>
```

- 4. 运行结果
