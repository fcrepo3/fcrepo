<%@ page info="200==OK" %>
<%@page isErrorPage="true" %>
<!-- http://java.sun.com/developer/EJTechTips/2003/tt0114.html -->
<%
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Type", "text/html; charset=UTF8");        
%>
<html><head>
   <title>Fedora: 200 OK</title></head>
   <body>
      <center>
ok
      </center>
   </body></html>

