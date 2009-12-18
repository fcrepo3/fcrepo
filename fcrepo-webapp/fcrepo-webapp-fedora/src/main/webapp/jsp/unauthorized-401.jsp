<%@ page info="401==unauthorized response; tomcat didn't get user credentials where web.xml says they are needed " %>
<%
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.addHeader("Content-Type", "text/html; charset=UTF8");        
        response.addHeader("WWW-Authenticate", "BASIC realm=\"Fedora Repository Server\"");
%>
<html><head>
      <title>Fedora: 401 Unauthorized</title></head>
   <body>
      <center>
         <table border="0" cellpadding="0" cellspacing="0" width="784">
            <tbody><tr>
               <td height="134" valign="top" width="141"><img src="<%= request.getContextPath() %>/images/newlogo2.jpg" height="134" width="141"></td>
               <td valign="top" width="643">
                  <center>
                     <h2>401 Unauthorized</h2>
                     <h3>Authentication failed</h3>
                  </center>
               </td>
            </tr>
         </tbody></table>
      </center>
   </body></html>
