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