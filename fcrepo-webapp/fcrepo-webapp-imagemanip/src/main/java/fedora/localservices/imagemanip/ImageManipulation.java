/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */

/**
 * Image manipulations (with the exception of the watermarking function)
 * are handled by ImageJ, a Java API written for image processing.
 *
 * Image encoding and decoding is handled by JAI, the Java Advanced Imaging
 * API, with the exception of GIF encoding, which is handled by ImageJ.
 *
 *  ImageJ Information:
 *
 *  Rasband, W.S., ImageJ, National Institutes of Health, Bethesda,
 *  Maryland, USA, http://rsb.info.nih.gov/ij/, 1997-2003.
 *
 *    The GifEncoder portion of ImageJ is copyrighted below:
 *
 *  Transparency handling and variable bit size courtesy of Jack Palevich.
 *
 *  Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 *  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  Visit the ACME Labs Java page for up-to-date versions of this and other
 *  fine Java utilities: http://www.acme.com/java/
 *
 */
package fedora.localservices.imagemanip;

import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.GifEncoder;
import ij.process.ImageProcessor;
import ij.process.MedianCut;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.media.jai.JAI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;

import com.sun.media.jai.codec.BMPEncodeParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.PNGEncodeParam;
import com.sun.media.jai.codec.TIFFEncodeParam;

/**
 * ImageManipulation is a Java servlet that takes a URL of an image as a param
 * and based on other given parameters, can perform a variety of image related
 * manipulations on the object.
 * 
 * <p>After the image is manipulated, it is then sent back as an image/type
 * object to the calling parent, most often a browser or an HTML img tag.
 * 
 * @author Theodore Serbinski
 */
public class ImageManipulation
        extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private String inputMimeType;

    private boolean alreadyConvertedToRGB = false;

    private final MultiThreadedHttpConnectionManager cManager =
            new MultiThreadedHttpConnectionManager();

    /**
     * Method automatically called by browser to handle image manipulations.
     * 
     * @param req
     *        Browser request to servlet res Response sent back to browser after
     *        image manipulation
     * @throws IOException
     *         If an input or output exception occurred ServletException If a
     *         servlet exception occurred
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
    	System.setProperty("java.awt.headless", "true");
        // collect all possible parameters for servlet
        String url = req.getParameter("url");
        String op = req.getParameter("op");
        String newWidth = req.getParameter("newWidth");
        String brightAmt = req.getParameter("brightAmt");
        String zoomAmt = req.getParameter("zoomAmt");
        String wmText = req.getParameter("wmText");
        String cropX = req.getParameter("cropX");
        String cropY = req.getParameter("cropY");
        String cropWidth = req.getParameter("cropWidth");
        String cropHeight = req.getParameter("cropHeight");
        String convertTo = req.getParameter("convertTo");
        if (convertTo != null) {
            convertTo = convertTo.toLowerCase();
        }
        try {
            if (op == null) {
                throw new ServletException("op parameter not specified.");
            }
            String outputMimeType;
            // get the image via url and put it into the ImagePlus processor.
            BufferedImage img = getImage(url);
            // do watermarking stuff
            if (op.equals("watermark")) {
                if (wmText == null) {
                    throw new ServletException("Must specify wmText.");
                }
                Graphics g = img.getGraphics();
                int fontSize = img.getWidth() * 3 / 100;
                if (fontSize < 10) {
                    fontSize = 10;
                }
                g.setFont(new Font("Lucida Sans", Font.BOLD, fontSize));
                FontMetrics fm = g.getFontMetrics();
                int stringWidth =
                        (int) fm.getStringBounds(wmText, g).getWidth();
                int x = img.getWidth() / 2 - stringWidth / 2;
                int y = img.getHeight() - fm.getHeight();
                g.setColor(new Color(180, 180, 180));
                g.fill3DRect(x - 10,
                             y - fm.getHeight() - 4,
                             stringWidth + 20,
                             fm.getHeight() + 12,
                             true);
                g.setColor(new Color(100, 100, 100));
                g.drawString(wmText, x + 2, y + 2);
                g.setColor(new Color(240, 240, 240));
                g.drawString(wmText, x, y);
            }
            ImageProcessor ip = new ImagePlus("temp", img).getProcessor();
            // if the inputMimeType is image/gif, need to convert to RGB in any case
            if (inputMimeType.equals("image/gif")) {
                ip = ip.convertToRGB();
                alreadyConvertedToRGB = true;
            }
            // causes scale() and resize() to do bilinear interpolation
            ip.setInterpolate(true);
            if (!op.equals("convert")) {
                if (op.equals("resize")) {
                    ip = resize(ip, newWidth);
                } else if (op.equals("zoom")) {
                    ip = zoom(ip, zoomAmt);
                } else if (op.equals("brightness")) {
                    ip = brightness(ip, brightAmt);
                } else if (op.equals("watermark")) {
                    // this is now taken care of beforehand (see above)
                } else if (op.equals("grayscale")) {
                    ip = grayscale(ip);
                } else if (op.equals("crop")) {
                    ip = crop(ip, cropX, cropY, cropWidth, cropHeight);
                } else {
                    throw new ServletException("Invalid operation: " + op);
                }
                outputMimeType = inputMimeType;
            } else {
                if (convertTo == null) {
                    throw new ServletException("Neither op nor convertTo was specified.");
                }
                if (convertTo.equals("jpg") || convertTo.equals("jpeg")) {
                    outputMimeType = "image/jpeg";
                } else if (convertTo.equals("gif")) {
                    outputMimeType = "image/gif";
                } else if (convertTo.equals("tiff")) {
                    outputMimeType = "image/tiff";
                } else if (convertTo.equals("bmp")) {
                    outputMimeType = "image/bmp";
                } else if (convertTo.equals("png")) {
                    outputMimeType = "image/png";
                } else {
                    throw new ServletException("Invalid format: " + convertTo);
                }
            }
            res.setContentType(outputMimeType);
            BufferedOutputStream out =
                    new BufferedOutputStream(res.getOutputStream());
            outputImage(ip, out, outputMimeType);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
                    .getClass().getName()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Gets and deserializes the image at the given URL into an Image object.
     * This method also sets the inputMimeType based on the HTTP Content-Type
     * header so that, if the image needs to be returned in it's original
     * format, the correct mime type can be sent in the response header. If the
     * input image is not a gif, jpg, tiff, bmp, or png (according to the http
     * response header), or some other kind of error occurs while reading the
     * stream from the remote host, a ServletException is thrown.
     * 
     * @param url
     *        The location of the input image.
     * @return Image The image object, if successful.
     * @throws Exception
     *         If any of the aforementioned problems occurs.
     */
    private BufferedImage getImage(String url) throws Exception {
        GetMethod get = null;
        try {
            cManager.getParams().setConnectionTimeout(20000);
            HttpClient client = new HttpClient(cManager);
            get = new GetMethod(url);
            get.setFollowRedirects(true);
            int resultCode = client.executeMethod(get);
            if (resultCode != 200) {
                throw new ServletException("Could not load image: " + url
                        + ".  Errorcode " + resultCode + " from remote server.");
            }
            inputMimeType = get.getResponseHeader("Content-Type").getValue();
            if (inputMimeType.equals("image/gif")
                    || inputMimeType.equals("image/jpeg")
                    || inputMimeType.equals("image/tiff")
                    || inputMimeType.equals("image/bmp")
                    || inputMimeType.equals("image/x-ms-bmp")
                    || inputMimeType.equals("image/x-bitmap")
                    || inputMimeType.equals("image/png")) {
                if (inputMimeType.endsWith("p")) {
                    inputMimeType = "image/bmp"; // windows bitmaps are most
                }
                // commonly supported with this
                // mime type, even though it's not
                // an IANA-registered image type
                return JAI.create("stream",
                                  new MemoryCacheSeekableStream(get
                                          .getResponseBodyAsStream()))
                        .getAsBufferedImage();
            } else {
                throw new ServletException("Source image was not a gif, png, "
                        + "bmp, tiff, or jpg.");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }

    }

    private void outputImage(ImageProcessor ip,
                             OutputStream out,
                             String outputMimeType) throws Exception {
        if (outputMimeType.equals("image/gif")) {
            if (!alreadyConvertedToRGB) {
                ip = ip.convertToRGB();
            }
            MedianCut mc =
                    new MedianCut((int[]) ip.getPixels(), ip.getWidth(), ip
                            .getHeight());
            ip = mc.convertToByte(256);
            ImagePlus imp = new ImagePlus("temp", ip);
            FileInfo fi = imp.getFileInfo();
            byte pixels[] = (byte[]) imp.getProcessor().getPixels();
            GifEncoder ge =
                    new GifEncoder(fi.width,
                                   fi.height,
                                   pixels,
                                   fi.reds,
                                   fi.greens,
                                   fi.blues);
            ge.write(out);
        } else {
            ImageEncodeParam param = null;
            String format = null;
            if (outputMimeType.equals("image/jpeg")) {
                param = new JPEGEncodeParam();
                format = "JPEG";
            } else if (outputMimeType.equals("image/tiff")) {
                param = new TIFFEncodeParam();
                format = "TIFF";
            } else if (outputMimeType.equals("image/bmp")) {
                param = new BMPEncodeParam();
                format = "BMP";
            } else if (outputMimeType.equals("image/png")) {
                param = new PNGEncodeParam.RGB();
                format = "PNG";
            }
            ImageCodec.createImageEncoder(format, out, param).encode(JAI
                    .create("AWTImage", ip.createImage()));
        }
    }

    /**
     * Resizes an image to the supplied new width in pixels. The height is
     * reduced proportionally to the new width.
     * 
     * @param ip
     *        The image to resize newWidth The width in pixels to resize the
     *        image to
     * @return The image resized
     */
    private ImageProcessor resize(ImageProcessor ip, String newWidth) {
        if (newWidth != null) {
            try {
                int width = Integer.parseInt(newWidth);

                if (width < 0) {
                    return ip;
                }

                int imgWidth = ip.getWidth();
                int imgHeight = ip.getHeight();

                ip = ip.resize(width, width * imgHeight / imgWidth);
            }
            // no need to do anything with number format exception since the servlet
            // returns only images; just return the original image
            catch (NumberFormatException e) {
            }
        }

        return ip;
    }

    /**
     * Zooms either in or out of an image by a supplied amount. The zooming
     * occurs from the center of the image.
     * 
     * @param ip
     *        The image to zoom zoomAmt The amount to zoom the image. 0 <
     *        zoomAmt < 1 : zoom out 1 = zoomAmt : original image 1 < zoomAmt :
     *        zoom in
     * @return The image zoomed
     */
    private ImageProcessor zoom(ImageProcessor ip, String zoomAmt) {
        if (zoomAmt != null) {
            try {
                float zoom = Float.parseFloat(zoomAmt);

                if (zoom < 0) {
                    return ip;
                }

                ip.scale(zoom, zoom);

                // if the image is being zoomed out, trim the extra whitespace around the image
                if (zoom < 1) {
                    int imgWidth = ip.getWidth();
                    int imgHeight = ip.getHeight();

                    // set a ROI around the image, minus the extra whitespace
                    ip.setRoi(Math.round(imgWidth / 2 - imgWidth * zoom / 2),
                              Math.round(imgHeight / 2 - imgHeight * zoom / 2),
                              Math.round(imgWidth * zoom),
                              Math.round(imgHeight * zoom));
                    ip = ip.crop();
                }

            }

            // no need to do anything with number format exception since the servlet
            // returns only images; just return the original image
            catch (NumberFormatException e) {
            }
        }

        return ip;
    }

    /**
     * Adjusts the brightness of an image by a supplied amount.
     * 
     * @param ip
     *        The image to adjust the brightness of brightAmt The amount to
     *        adjust the brightness of the image by 0 <= brightAmt < 1 : darkens
     *        image 1 = brightAmt : original image 1 < brightAmt : brightens
     *        image
     * @return The image with brightness levels adjusted
     */
    private ImageProcessor brightness(ImageProcessor ip, String brightAmt) {
        if (brightAmt != null) {
            try {
                float bright = Float.parseFloat(brightAmt);

                if (bright < 0) {
                    return ip;
                }

                ip.multiply(bright);

            }

            // no need to do anything with number format exception since the servlet
            // returns only images; just return the original image
            catch (NumberFormatException e) {
            }
        }

        return ip;
    }

    /**
     * Converts an image to gray scale.
     * 
     * @param ip
     *        The image to convert to grayscale
     * @return The image converted to grayscale
     */
    private ImageProcessor grayscale(ImageProcessor ip) {
        ip = ip.convertToByte(true);

        return ip;
    }

    /**
     * Crops an image with supplied starting point and ending point.
     * 
     * @param ip
     *        The image to crop cropX The starting x position; x=0 corresponds
     *        to left side of image cropY The starting y position; y=0
     *        corresponds to top of image cropWidth The width of the crop,
     *        starting from the above x cropHeight The height of the crop,
     *        starting from the above y
     * @return The image cropped
     */
    public ImageProcessor crop(ImageProcessor ip,
                               String cropX,
                               String cropY,
                               String cropWidth,
                               String cropHeight) {
        if (cropX != null && cropY != null) {
            try {
                int x = Integer.parseInt(cropX);
                int y = Integer.parseInt(cropY);
                int width;
                int height;

                // if value for cropWidth is not given, just use the width of the image
                if (cropWidth != null) {
                    width = Integer.parseInt(cropWidth);
                } else {
                    width = ip.getWidth();
                }

                // if value for cropHeight is not given, just use the height of the image
                if (cropHeight != null) {
                    height = Integer.parseInt(cropHeight);
                } else {
                    height = ip.getHeight();
                }

                // if any value is negative, this causes ImageJ to explode, so just return
                if (x < 0 || y < 0 || width < 0 || height < 0) {
                    return ip;
                }

                ip.setRoi(x, y, width, height);
                ip = ip.crop();
            }

            // no need to do anything with number format exception since the servlet
            // returns only images; just return the original image
            catch (NumberFormatException e) {
            }
        }

        return ip;
    }
}
