<%@ page info="100==continue response; use to signal authz success w/o performing action" %>
<%
        response.setStatus(HttpServletResponse.SC_CONTINUE);
        response.addHeader("Content-Type", "text/html; charset=UTF8");        
%>
<html><head>
      <title>Fedora: 100 Continue</title></head>
   <body>
      <center>
         <table border="0" cellpadding="0" cellspacing="0" width="784">
            <tbody><tr>
               <td height="134" valign="top" width="141"><img src="<%= request.getContextPath() %>/images/newlogo2.jpg" height="134" width="141"></td>
               <td valign="top" width="643">
                  <center>
                     <h2>100 Continue</h2>
                     <h3>Authorization successful</h3>
                  </center>
               </td>
            </tr>
         </tbody></table>
      </center>
   </body></html>
