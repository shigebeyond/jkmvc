<?php
use php\jkmvc\http\IController;
use php\jkmvc\http\HttpRequest;

class Test extends IController{

    function index(){
        $req = $this->req;
        //$req = HttpRequest::current();
        echo $req->controller() . '-' . $req->action();
    }
}