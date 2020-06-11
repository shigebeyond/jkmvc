package net.jkcode.jkmvc.example.controller

import net.jkcode.jkutil.common.format
import net.jkcode.jkutil.common.httpLogger
import net.jkcode.jkmvc.example.model.UserModel
import net.jkcode.jkmvc.http.controller.Controller
import net.jkcode.jkmvc.http.fromRequest
import net.jkcode.jkmvc.http.isPost
import net.jkcode.jkmvc.http.isUpload
import net.jkcode.jkmvc.http.session.Auth
import net.jkcode.jkmvc.orm.OrmQueryBuilder
import net.jkcode.jkmvc.orm.isLoaded
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
    public override fun after(result: Any?, t: Throwable?): Any? {
        // 如记录日志
        httpLogger.info("action后置处理")
        return super.after(result, t)
    }

    /**
     * 列表页
     * list page
     */
    public fun index()
    {
        val query: OrmQueryBuilder = UserModel.queryBuilder()
        // 统计用户个数 | count users
        val counter:OrmQueryBuilder = query.clone() as OrmQueryBuilder // 复制query builder
        val count = counter.count()
        // 查询所有用户 | find all users
        val users = query.findModels<UserModel>()
        // 渲染视图 | render view
        res.renderView(view("user/index", mapOf("count" to count, "users" to users)))
    }

    /**
     * 详情页
     * detail page
     */
    public fun detail()
    {
        // 获得路由参数id: 2种写法 | 2 ways to get route parameter: "id"
        // val id = req.getInt("id");
        val id:Int? = req["id"] // req["xxx"]
        // 查询单个用户 | find a user
        //val user = UserModel.queryBuilder().where("id", id).findModel<UserModel>()
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderHtml("用户[$id]不存在")
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
    public fun new()
    {
        // 处理请求 | handle request
        if(req.isPost){ //  post请求：保存表单数据 | post request: save form data
            // 创建空的用户 | create user model
            val user = UserModel()
            // 获得请求参数：3种写法 | 3 ways to get request parameter
            /* // 1 req.getParameter("xxx");
            user.name = req.getParameter("name")!!;
            user.age = req.getInt("age", 0)!!; // 带默认值 | default value
            */
            // 2 req["xxx"]
            user.name = req["name"]!!;
            user.age = req["age"]!!;

            // 3 Orm.fromRequest(req)
            user.fromRequest(req)
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
    public fun edit()
    {
        // 查询单个用户 | find a user
        val id: Int = req["id"]!!
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderHtml("用户[" + req["id"] + "]不存在")
            return
        }
        // 处理请求 | handle request
        if(req.isPost){ //  post请求：保存表单数据 | post request: save form data
            // 获得请求参数：3种写法 | 3 way to get request parameter
            /* // 1 req.getParameter("xxx");
            user.name = req.getParameter("name")!!;
            user.age = req.getInt("age", 0)!!; // 带默认值 | default value
            */
            /*// 2 req["xxx"]
            user.name = req["name"]!!;
            user.age = req["age"]!!;
            */
            // 3 Orm.fromRequest(req)
            user.fromRequest(req)
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
    public fun delete()
    {
        val id:Int? = req["id"]
        // 查询单个用户 | find a user
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderHtml("用户[$id]不存在")
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
    public fun uploadAvatar()
    {
        // 查询单个用户 | find a user
        val id: Int = req["id"]!!
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.renderHtml("用户[" + req["id"] + "]不存在")
            return
        }

        // 检查并处理上传文件 | check and handle upload request
        if(req.isUpload){ // 检查上传请求 | check upload request
            user.avatar = req.storePartFileAndGetRelativePath("avatar")!!
            user.update()
        }

        // 重定向到详情页 | redirect to detail page
        redirect("user/detail/$id");
    }

    /**
     * 登录
     */
    public fun login(){
        if(req.isPost){ // post请求
            val user = Auth.instance().login(req["username"]!!, req["password"]!!);
            if(user == null)
                res.renderHtml("登录失败")
            else
                redirect("user/login")
        }else{ // get请求
            res.renderView(view())
        }
    }

    /**
     * 登录
     */
    public fun logout(){
        Auth.instance().logout()
        redirect("user/login")
    }
}