<?php
use php\jkmvc\http\IController;
use php\jkmvc\http\HttpRequest;
use php\jkmvc\http\HttpRequest;
use php\lang\CompletableFuture;

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
        echo 'all params: ' . print_r($req->params(), true) . "\n";
    }

    /**
     * http://localhost:8080/jkmvc-example/$test/login
     */
    function login(){
        $this->view('login', ['msg' => '请输入登录账户和密码']);
    }

    /**
     * http://localhost:8080/jkmvc-example/$test/transferGit
     */
    function transferGit(){
        return $this->transferAndReturn("https://search.gitee.com/?skin=rec&type=repository&q=jkmvc");
    }

    /**
     * http://localhost:8080/jkmvc-example/$test/joinFuture
     */
    function joinFuture(){
        $f1 = $this->transfer("http://platinum.shikee.com/data/34519102");
        $f2 = $this->transfer("http://platinum.shikee.com/data/34499569");
        $f = CompletableFuture::join([$f1, $f2]);
        var_dump($f->get());
    }
}