package com.jkmvc.example.controller

import com.jkmvc.common.format
import com.jkmvc.example.model.UserModel
import com.jkmvc.http.Controller
import com.jkmvc.http.httpLogger
import com.jkmvc.http.valuesFromRequest
import com.jkmvc.orm.OrmQueryBuilder
import com.jkmvc.orm.isLoaded
import com.jkmvc.session.Auth
import java.util.*


/**
 * 用户管理
 * user manage
 */
class UserController: Controller()
{
    /**
     * action前置处理
     */
    public override fun before() {
        // 如检查权限
        httpLogger.info("action前置处理")
    }

    /**
     * action后置处理
     */
    public override fun after() {
        // 如记录日志
        httpLogger.info("action后置处理")
    }

    /**
     * 列表页
     * list page
     */
    public fun indexAction()
    {
        val query: OrmQueryBuilder = UserModel.queryBuilder()
        // 统计用户个数 | count users
        val counter:OrmQueryBuilder = query.clone() as OrmQueryBuilder // 复制query builder
        val count = counter.count()
        // 查询所有用户 | find all users
        val users = query.findAllModels<UserModel>()
        // 渲染视图 | render view
        res.renderView(view("user/index", mutableMapOf("count" to count, "users" to users)))
    }

    /**
     * 详情页
     * detail page
     */
    public fun detailAction()
    {
        // 获得路由参数id: 2种写法 | 2 ways to get route parameter: "id"
        // val id = req.getIntRouteParameter("id"); // req.getRouteParameter["xxx"]
        val id:Int? = req["id"] // req["xxx"]
        // 查询单个用户 | find a user
        //val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderString("用户[$id]不存在")
            return
        }
        // 渲染视图 | render view
        val view = view("user/detail")
        view["user"] = user; // 设置视图参数 | set view data
        res.renderView(view)
    }

    /**
     * 新建页
     * new page
     */
    public fun newAction()
    {
        // 处理请求 | handle request
        if(req.isPost()){ //  post请求：保存表单数据 | post request: save form data
            // 创建空的用户 | create user model
            val user = UserModel()
            // 获得请求参数：3种写法 | 3 ways to get request parameter
            /* // 1 req.getParameter("xxx");
            user.name = req.getParameter("name")!!;
            user.age = req.getIntParameter("age", 0)!!; // 带默认值 | default value
            */
            // 2 req["xxx"]
            user.name = req["name"]!!;
            user.age = req["age"]!!;

            // 3 Orm.valuesFromRequest(req)
            user.valuesFromRequest(req)
            user.create(); // create user
            // 重定向到列表页 | redirect to list page
            redirect("user/index");
        }else{ // get请求： 渲染视图 | get request: render view
            val view = view() // 默认视图为action名： user/new | default view's name = action：　user/new
            res.renderView(view)
        }
    }

    /**
     * 编辑页
     * edit page
     */
    public fun editAction()
    {
        // 查询单个用户 | find a user
        val id: Int = req["id"]!!
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderString("用户[" + req["id"] + "]不存在")
            return
        }
        // 处理请求 | handle request
        if(req.isPost()){ //  post请求：保存表单数据 | post request: save form data
            // 获得请求参数：3种写法 | 3 way to get request parameter
            /* // 1 req.getParameter("xxx");
            user.name = req.getParameter("name")!!;
            user.age = req.getIntParameter("age", 0)!!; // 带默认值 | default value
            */
            /*// 2 req["xxx"]
            user.name = req["name"]!!;
            user.age = req["age"]!!;
            */
            // 3 Orm.valuesFromRequest(req)
            user.valuesFromRequest(req)
            user.update() // update user
            // 重定向到列表页 | redirect to list page
            redirect("user/index");
        }else{ // get请求： 渲染视图 | get request: render view
            val view = view() // 默认视图为action名： user/edit | default view's name = action：　user/edit
            view["user"] = user; // 设置视图参数 |  set view data
            res.renderView(view)
        }
    }

    /**
     * 删除
     * delete action
     */
    public fun deleteAction()
    {
        val id:Int? = req["id"]
        // 查询单个用户 | find a user
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderString("用户[$id]不存在")
            return
        }
        // 删除 | delete user
        user.delete();
        // 重定向到列表页 | redirect to list page
        redirect("user/index");
    }

    /**
     * 上传头像
     * upload avatar
     */
    public fun uploadAvatarAction()
    {
        // 设置上传的子目录（将上传文件保存到指定的子目录），必须要在调用 req 的其他api之前调用，否则无法生效
        // set uploadSubdir which uploaded file is saved, you must set it before calling req's other api, or it's useless
        req.uploadSubdir = "avatar/" + Date().format("yyyy/MM/dd")

        // 查询单个用户 | find a user
        val id: Int = req["id"]!!
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderString("用户[" + req["id"] + "]不存在")
            return
        }

        // 检查并处理上传文件 | check and handle upload request
        if(req.isUpload()){ // 检查上传请求 | check upload request
            user.avatar = req.getFileRelativePath("avatar")
            user.update()
        }

        // 重定向到详情页 | redirect to detail page
        redirect("user/detail/$id");
    }

    /**
     * 登录
     */
    public fun loginAction(){
        if(req.isPost()){ // post请求
            val user = Auth.instance().login(req["username"]!!, req["password"]!!);
            if(user == null)
                res.renderString("登录失败")
            else
                redirect("user/login")
        }else{ // get请求
            res.renderView(view())
        }
    }

    /**
     * 登录
     */
    public fun logoutAction(){
        Auth.instance().logout()
        redirect("user/login")
    }
}