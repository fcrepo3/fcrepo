/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package fedora.server.rest;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.server.impl.model.HttpHelper;

import fedora.server.utilities.DateUtility;

public class RestHelper {
    static Date toDate(String s) {
        return DateUtility.convertStringToDate(s);
    }

    static boolean toBoolean(String s) {
        return (Boolean.TRUE == Boolean.parseBoolean(s));
    }

    static String getPID(HttpServletRequest request) {
        return null;
    }

    static String getDSID(HttpServletRequest request) {
        return null;
    }

    /**
     * Return the first argument that is not null. This is used to specify
     * default parameters
     *
     * @param args
     * @return the first argument that is not null.
     */
    static <T> T firstNotNull(T... args) {
        for (T arg : args) {
            if (arg != null) {
                return arg;
            }
        }

        return null;
    }

    /**
     * Return the submitted content type, either through a "format" parameter,
     * the HTTP content type header. If the content type is application/xml",
     * return "text/xml".
     *
     * @param request
     * @return content type
     */
    static String getConsumedContentType(HttpServletRequest request) {
        String mimeType = firstNotNull(request.getParameter("format"), request
                .getContentType());

        return ((mimeType != null) ? mimeType.replace("application/xml",
                BaseRestResource.XML) : null);
    }

    static String getRequired(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    static String getOptional(HttpServletRequest request, String name) {
        return RestHelper.getOptional(request, name, null);
    }

    static String getOptional(HttpServletRequest request, String name, String defaultValue) {
        return firstNotNull(request.getParameter(name), defaultValue);
    }

    static String[] getOptionalValues(HttpServletRequest request, String name) {
        return firstNotNull(request.getParameterValues(name),
                BaseRestResource.EMPTY_STRING_ARRAY);
    }

    /**
     * Return the content type represented by the given string.  If the string does not have a "/", assume
     * it's "text/{format}".  For example, if format is "xml", the returned media type will be "text/xml".
     *
     * @param format
     * @return the content type represented by the given string
     */
    public static MediaType getContentType(String format) {
        if (format == null) {
            format = "text/html";
        } else if (format.indexOf('/') <= 0) {
            format = "text/" + format;
        }

        return HttpHelper.getContentType(format);
    }
}
