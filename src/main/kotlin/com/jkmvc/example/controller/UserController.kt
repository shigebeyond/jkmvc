package com.jkmvc.example.controller

import com.jkmvc.example.model.UserModel
import com.jkmvc.http.Controller
import com.jkmvc.orm.isLoaded

/**
 * 用户管理
 */
class UserController: Controller()
{
    /**
     * 列表页
     */
    public fun actionIndex()
    {
        // 查询所有用户
        val users = UserModel.queryBuilder().findAll<UserModel>()
        // 渲染视图
        res.render(view("index", mutableMapOf("users" to users)))
    }

    /**
     * 详情页
     */
    public fun actionDetail()
    {
        // 获得路由参数id
        val id = req.getIntRouteParameter("id");
        // 查询单个用户
        //val user = UserModel.queryBuilder().where("id", id).find<UserModel>()
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.render("用户[$id]不存在")
            return
        }
        // 渲染视图
        val view = view("detail")
        view["user"] = user; // 设置视图参数
        res.render(view)
    }

    /**
     * 新建页
     */
    public fun actionNew()
    {
        // 创建空的用户
        val user = UserModel()
        // 处理请求
        if(req.isPost()){ //  post请求：保存表单数据
            user.name = "li";
            user.age = 13;
            user.create();
            // 重定向到列表页
            redirect("user/index");
        }else{ // get请求： 渲染视图
            val view = view() // 默认视图为action名： new
            view["user"] = user; // 设置视图参数
            res.render(view)
        }
    }

    /**
     * 编辑页
     */
    public fun actionEdit()
    {
        // 获得路由参数id
        val id = req.getIntRouteParameter("id");
        // 查询单个用户
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.render("用户[$id]不存在")
            return
        }
        // 处理请求
        if(req.isPost()){ //  post请求：保存表单数据
            user.name = "li";
            user.age = 13;
            user.update();
            // 重定向到列表页
            redirect("user/index");
        }else{ // get请求： 渲染视图
            val view = view() // 默认视图为action名： edit
            view["user"] = user; // 设置视图参数
            res.render(view)
        }
    }

    /**
     * 删除
     */
    public fun actionDelete()
    {
        // 获得路由参数id
        val id = req.getIntRouteParameter("id");
        // 查询单个用户
        val user = UserModel(id)
        if(!user.isLoaded()){
            res.render("用户[$id]不存在")
            return
        }
        // 删除
        user.delete();
        // 重定向到列表页
        redirect("user/index");
    }

}