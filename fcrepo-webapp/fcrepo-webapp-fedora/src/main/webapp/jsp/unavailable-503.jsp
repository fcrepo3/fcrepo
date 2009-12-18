<%@ page info="503==Unavailable" %>
<%@page isErrorPage="true" %>
<%@page import="java.io.StringWriter, java.io.PrintWriter" %>
<!-- http://java.sun.com/developer/EJTechTips/2003/tt0114.html -->
<%
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.addHeader("Content-Type", "text/html; charset=UTF8");        
%>
<html><head>
      <title>Fedora: 503 Unavailable</title></head>
   <body>
      <center>
         <table border="0" cellpadding="0" cellspacing="0" width="784">
            <tbody><tr>
               <td height="134" valign="top" width="141"><img src="<%= request.getContextPath() %>/images/newlogo2.jpg" height="134" width="141"></td>
               <td valign="top" width="643">
                  <center>
                     <h2>503 Unavailable</h2>
                  </center>
<%
if (exception != null && exception.getMessage() != null) {
    out.print(exception.getMessage());
}
%>
               </td>
            </tr>

<tr>
<td colspan="2">
<hr size="1"/>
<pre>
<%
StringWriter sw = new StringWriter();
PrintWriter pw = new PrintWriter(sw);
exception.printStackTrace(pw);
out.print(sw);
sw.close();
pw.close();
%>
</pre>
</td></tr>
         </tbody></table>

</center>
   </body></html>
