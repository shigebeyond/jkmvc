# 上传文件

jkmvc对上传包`servlets.com:cos:05Nov2002`进行了二次封装，并提供了便捷的api来处理上传文件。

但是考虑到上传的

## 1 上传配置

vim src/main/resources/upload.properties

```
# 上传文件的保存目录，末尾不要带/
uploadDirectory=upload
# 上传文件的大小限制，单位 B K M G T
maxPostSize=1M
# 编码
encoding=gbk
# 访问上传文件的域名
#uploadDomain=http://localhost:8081/jkmvc/upload
```

配置项 | 作用
--- | ---
uploadDirectory | 上传的根目录，由jkmvc接收的上传文件都保存到该目录下，同时为了能访问这些文件，你需要基于该目录建立http文件服务器
uploadDomain | 访问上传文件的域名，结合它可以获得访问上传文件的url

## 2 处理上传文件

### 2.1 上传的表单

```
<form class="form-inline" action="<%= req.absoluteUrl("user/uploadAvatar/" + user.getId()) %>" method="post" enctype="multipart/form-data">
    <div class="form-group">
        <label for="avatar">avatar</label>
        <input type="file" class="form-control" id="avatar" placeholder="avatar" name="avatar">
    </div>
    <button type="submit" class="btn btn-default">Upload</button>
</form>
```

### 2.2 接收上传文件

```
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
        res.render("用户[" + req["id"] + "]不存在")
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
```