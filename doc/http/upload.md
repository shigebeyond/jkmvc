# Upload files

Jkmvc uses `servlets.com: cos: 05Nov2002` for uploading files, and improves into a convenient API to handle the upload file.

## 1 Upload configuration

vim src/main/resources/upload.properties

```
# upload directory, where the uploaded file save, without postfix "/"
uploadRootDirectory=/var/www/upload
encoding=gbk
# forbidden file extensions, splited by `,`
forbiddenExt = jsp,jspx,exe,sh,php,py
# domain to visit uploaded file
uploadDomain=http://localhost:8081/jkmvc/upload
```

Configuration item | usage
--- | ---
uploadRootDirectory | The root directory for upload, which jkmvc saves the upload files. To access these files, you need to setup a file server on this diretory.
uploadDomain | Domain name to access to upload files

## 2 Process upload files

### 2.1 upload form

The form is defined with `enctype =" multipart / form-data "` property.

```
<form class="form-inline" action="<%= req.absoluteUrl("user/uploadAvatar/" + user.getId()) %>" method="post" enctype="multipart/form-data">
    <div class="form-group">
        <label for="avatar">avatar</label>
        <input type="file" class="form-control" id="avatar" placeholder="avatar" name="avatar">
    </div>
    <button type="submit" class="btn btn-default">Upload</button>
</form>
```

### 2.2 Receive upload files

```
/**
 * upload avatar
 */
public fun uploadAvatar()
{
    // find a user
    val id: Int = req["id"]!!
    val user = UserModel(id)
    if(!user.isLoaded()){
        res.renderHtml("use [" + req["id"] + "] not exists")
        return
    }

    // check and handle upload request
    if(req.isUpload){ // check upload request
        user.avatar = req.storePartFileAndGetRelativePath("avatar")
        user.update()
    }

    // redirect to detail page
    redirect("user/detail/$id");
}
```

## 3 Download file

After the file upload, it needs to be downloaded. There are 2 ways to download the file:

### 3.1 Download by java

Call `res.render (file: File)` in the Controller, to return file to the browser

### 3.2 Download by the file server

We can use apache / nginx to provide file download service, by specify the upload directory as http root directory

```
location ~ \.(gif|jpg|jpeg|.js|.css)$ {
    root   /var/www/upload;
    index  index.html index.htm;
}

```

