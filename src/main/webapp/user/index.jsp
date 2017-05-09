<%@ page language="java" import="java.util.*,com.jkmvc.http.Request,com.jkmvc.example.model.UserModel" pageEncoding="UTF-8"%>
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
    <div class="panel-heading">用户列表</div>
    <div class="panel-body">
      <p>...</p>
    </div>

    <!-- Table -->
    <table class="table">
      <thead>
        <tr>
          <th>id</th>
          <th>name</th>
          <th>age</th>
          <th>操作:</th>
        </tr>
      </thead>
      <tbody>
        <% Request req = (Request) request;
            List<UserModel> users = (List<UserModel>)request.getAttribute("users");
            for(UserModel user : users.iterator()){ %>
            <tr>
              <th scope="row"><%= user.get("id", null) %></th>
              <td><%= user.get("name", null) %></td>
              <td><%= user.get("name", null) %></td>
              <td>
                <a href="<%= req.absoluteUrl("user/show/" + user.get("id", null)) %>" class="btn btn-default">详情</a>
                <a href="<%= req.absoluteUrl("user/edit/" + user.get("id", null)) %>" class="btn btn-warn">编辑</a>
                <a href="<%= req.absoluteUrl("user/delete/" + user.get("id", null))%>" class="btn btn-error">删除</a>
               </td>
            </tr>
         <% } %>
      </tbody>
    </table>
  </div>
  <!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
  <script src="https://cdn.bootcss.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
</body>
</html>