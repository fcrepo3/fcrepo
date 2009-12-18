<%@ page info="403==forbidden response; xacml indicates that authorization should be denied" %>
<%
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.addHeader("Content-Type", "text/html; charset=UTF8");
%>
<html><head>
      <title>Fedora: 403 Forbidden</title></head>
   <body>
      <center>
         <table border="0" cellpadding="0" cellspacing="0" width="784">
            <tbody><tr>
               <td height="134" valign="top" width="141"><img src="<%= request.getContextPath() %>/images/newlogo2.jpg" height="134" width="141"></td>
               <td valign="top" width="643">
                  <center>
                     <h2>403 Forbidden</h2>
                     <h3>Authorization failed</h3>
                  </center>
               </td>
            </tr>
         </tbody></table>
      </center>
   </body></html>
