<%@ page language="java" import="java.util.*,net.jkcode.jkmvc.http.HttpRequest,net.jkcode.jkmvc.example.model.UserModel" pageEncoding="UTF-8"%>
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
    <div class="panel-heading">用户列表</div>
    <div class="panel-body">
      <p>
        <a href="<%= req.absoluteUrl("user/new/") %>" class="btn btn-warning">新建</a>
        <a href="<%= req.absoluteUrl("user/login/") %>" class="btn btn-danger">登录</a>
      </p>

       <!-- Table -->
      <table class="table">
        <thead>
          <tr>
            <th>id</th>
            <th>username</th>
            <th>password</th>
            <th>name</th>
            <th>age</th>
            <th>操作:</th>
          </tr>
        </thead>
        <tbody>
          <%  List<UserModel> users = (List<UserModel>)request.getAttribute("users");
              for (Iterator<UserModel> it = users.iterator(); it.hasNext();) {
               UserModel user = it.next(); %>
              <tr>
                <th scope="row"><%= user.getId() %></th>
                <td><%= user.getUsername() %></td>
                <td><%= user.getPassword() %></td>
                <td><%= user.getName() %></td>
                <td><%= user.getAge() %></td>
                <td>
                  <a href="<%= req.absoluteUrl("user/detail/" + user.getId()) %>" class="btn btn-default">详情</a>
                  <a href="<%= req.absoluteUrl("user/edit/" + user.getId()) %>" class="btn btn-primary">编辑</a>
                  <a href="<%= req.absoluteUrl("user/delete/" + user.getId())%>" class="btn btn-info">删除</a>
                 </td>
              </tr>
           <% } %>
        </tbody>
      </table>
    </div>
  </div>
  <!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
  <script src="https://cdn.bootcss.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
</body>
</html>