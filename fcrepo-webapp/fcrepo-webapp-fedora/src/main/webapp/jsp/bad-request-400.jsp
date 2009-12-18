<%@ page info="400==bad-request response; client error (parms, syntax, etc.)" %>
<%
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.addHeader("Content-Type", "text/html; charset=UTF8");        
%>
<html><head>
      <title>Fedora: 400 Bad Request</title></head>
   <body>
      <center>
         <table border="0" cellpadding="0" cellspacing="0" width="784">
            <tbody><tr>
               <td height="134" valign="top" width="141"><img src="<%= request.getContextPath() %>/images/newlogo2.jpg" height="134" width="141"></td>
               <td valign="top" width="643">
                  <center>
                     <h2>400 Bad Request</h2>
                     <h3>The syntax of the request is incorrect.</h3>
                  </center>
               </td>
            </tr>
         </tbody></table>
      </center>
   </body></html>
