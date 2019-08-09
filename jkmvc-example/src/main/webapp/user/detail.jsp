<%@ page language="java" import="net.jkcode.jkmvc.http.HttpRequest,net.jkcode.jkmvc.example.model.UserModel" pageEncoding="UTF-8"%>
<% HttpRequest req = HttpRequest.current(); %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>todolist</title>
  <!-- 最新版本的 Bootstrap 核心 CSS 文件 -->
  <link rel="stylesheet" href="https://cdn.bootcss.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
  <!-- 可选的 Bootstrap 主题文件（一般不用引入） -->
  <link rel="stylesheet" href="https://cdn.bootcss.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">
</head>
<body>
  <div class="panel panel-default">
    <!-- Default panel contents -->
    <div class="panel-heading">用户详情</div>

    <!-- Form -->
    <% UserModel user = (UserModel) req.getAttribute("user"); %>
    <div class="panel-body">
      <div class="form-group">
        <label for="id">id</label>
        <span><%= user.getId() %></span>
      </div>
      <div class="form-group">
        <label for="username">username</label>
        <span><%= user.getUsername() %></span>
      </div>
      <div class="form-group">
        <label for="password">password</label>
        <span><%= user.getPassword() %></span>
      </div>
      <div class="form-group">
        <label for="name">name</label>
        <span><%= user.getName() %></span>
      </div>
      <div class="form-group">
        <label for="age">Age</label>
        <span><%= user.getAge() %></span>
      </div>
      <% if(user.getAvatar() != null){ %>
          <div class="form-group">
             <label for="avatar">Avatar</label>
             <img src="<%= req.getUploadUrl(user.getAvatar())%>" width="150px" height="200px" >
           </div>
      <% } %>
      <form class="form-inline" action="<%= req.absoluteUrl("user/uploadAvatar/" + user.getId()) %>" method="post" enctype="multipart/form-data">
         <div class="form-group">
           <label for="avatar">avatar</label>
           <input type="file" class="form-control" id="avatar" placeholder="avatar" name="avatar" value="<%= user.getAvatar() %>">
         </div>
         <button type="submit" class="btn btn-default">Upload</button>
       </form>
    </div>

  </div>
  <!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
  <script src="https://cdn.bootcss.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
</body>
</html>