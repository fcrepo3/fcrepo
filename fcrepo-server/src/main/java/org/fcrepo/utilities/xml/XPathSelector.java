package org.fcrepo.utilities.xml;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Interface for doing efficient XPath selections. To obtain an implementation
 * of this interface use {@link DOM#createXPathSelector(String[])}.
 * <p/>
 * Note that your DOM must be constructed with namespaces explicitly enabled
 * for a namespace aware selector to work properly. You do this either via
 * the {@link DOM#streamToDOM(java.io.InputStream , boolean)} or
 * {@link DOM#stringToDOM(String, boolean)} methods or by calling
 * {@code setNamespaceAware(true)} on your {@code DocumentBuilderFactory}.
 */
public interface XPathSelector {

    /**
     * Extract an integer value from {@code node} or return {@code defaultValue}
     * if it is not found.
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the XPath to extract.
     * @param defaultValue the default value.
     * @return the value of the path, if existing, else
     *         defaultValue
     */
    public Integer selectInteger(Node node, String xpath, Integer defaultValue);


    /**
     * Extract an integer value from {@code node} or return {@code null} if it
     * is not found
     *
     * @param node  the node with the wanted attribute.
     * @param xpath the XPath to extract.
     * @return the value of the path or {@code null}
     */
    public Integer selectInteger(Node node, String xpath);

    /**
     * Extract a double precision floating point value from {@code node} or
     * return {@code defaultValue} if it is not found
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the XPath to extract.
     * @param defaultValue the default value.
     * @return the value of the path, if existing, else
     *         defaultValue
     */
    public Double selectDouble(Node node, String xpath, Double defaultValue);


    /**
     * Extract a double precision floating point value from {@code node} or
     * return {@code null} if it is not found
     *
     * @param node  the node with the wanted attribute.
     * @param xpath the XPath to extract.
     * @return the value of the path or {@code null}
     */
    public Double selectDouble(Node node, String xpath);

    /**
     * Extract a boolean value from {@code node} or return {@code defaultValue}
     * if there is no boolean value at {@code xpath}
     *
     * @param node         the node with the wanted attribute.
     * @param xpath        the path to extract.
     * @param defaultValue the default value.
     * @return the value of the path, if existing, else
     *         {@code defaultValue}
     */
    public Boolean selectBoolean(Node node, String xpath, Boolean defaultValue);

    /**
     * Extract a boolean value from {@code node} or return {@code false}
     * if there is no boolean value at {@code xpath}
     *
     * @param node  the node with the wanted attribute.
     * @param xpath the path to extract.
     * @return the value of the path, if existing, else
     *         {@code false}
     */
    public Boolean selectBoolean(Node node, String xpath);

    /**
     * Extract the given value from the node as a String or if the value cannot
     * be extracted, {@code defaultValue} is returned.
     * <p/>
     * Example: To get the value of the attribute "foo" in the node, specify
     * "@foo" as the path.
     * <p/>
     * Note: This method does not handle namespaces explicitely.
     *
     * @param node         the node with the wanted attribute
     * @param xpath        the XPath to extract.
     * @param defaultValue the default value
     * @return the value of the path, if existing, else
     *         {@code defaultValue}
     */
    public String selectString(Node node, String xpath, String defaultValue);

    /**
     * Extract the given value from the node as a String or if the value cannot
     * be extracted, the empty string is returned
     * <p/>
     * Example: To get the value of the attribute "foo" in the node, specify
     * "@foo" as the path.
     * <p/>
     * Note: This method does not handle namespaces explicitely.
     *
     * @param node  the node with the wanted attribute
     * @param xpath the XPath to extract
     * @return the value of the path, if existing, else
     *         the empty string
     */
    public String selectString(Node node, String xpath);

    /**
     * Select the Node list with the given XPath.
     * </p><p>
     * Note: This is a convenience method that logs exceptions instead of
     * throwing them.
     *
     * @param node  the root document.
     * @param xpath the xpath for the Node list.
     * @return the NodeList requested or an empty NodeList if unattainable
     */
    public NodeList selectNodeList(Node node, String xpath);

    /**
     * Select the Node with the given XPath.
     * </p><p>
     * Note: This is a convenience method that logs exceptions instead of
     * throwing them.
     *
     * @param dom   the root document.
     * @param xpath the xpath for the node.
     * @return the Node or null if unattainable.
     */
    public Node selectNode(Node dom, String xpath);


}
