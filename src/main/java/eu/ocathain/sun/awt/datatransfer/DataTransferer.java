/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package eu.ocathain.sun.awt.datatransfer;

import eu.ocathain.awt.datatransfer.DataFlavor;
//import eu.ocathain.awt.datatransfer.FlavorMap;
//import eu.ocathain.awt.datatransfer.FlavorTable;
//import eu.ocathain.awt.datatransfer.Transferable;
//import eu.ocathain.awt.datatransfer.UnsupportedFlavorException;

//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.Reader;
//import java.io.SequenceInputStream;
//import java.io.StringReader;
//
//import java.net.URI;
//import java.net.URISyntaxException;
//
//import java.nio.ByteBuffer;
//import java.nio.CharBuffer;
import java.nio.charset.Charset;
//import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//
//import java.security.AccessController;
//import java.security.PrivilegedAction;
//import java.security.PrivilegedActionException;
//import java.security.PrivilegedExceptionAction;
//import java.security.ProtectionDomain;

//import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
import java.util.Map;
//import java.util.SortedMap;
//import java.util.SortedSet;
//import java.util.Set;
//import java.util.Stack;
//import java.util.TreeMap;
//import java.util.TreeSet;
//
//import sun.awt.datatransfer.ToolkitThreadBlockedHandler;
//import sun.util.logging.PlatformLogger;
//
//import sun.awt.AppContext;
//import sun.awt.SunToolkit;
//
//
//import javax.imageio.ImageIO;
//import javax.imageio.ImageReader;
//import javax.imageio.ImageReadParam;
//import javax.imageio.ImageWriter;
//import javax.imageio.ImageTypeSpecifier;
//
//import javax.imageio.spi.ImageWriterSpi;
//
//import javax.imageio.stream.ImageInputStream;
//import javax.imageio.stream.ImageOutputStream;
//
//import sun.awt.image.ImageRepresentation;
//import sun.awt.image.ToolkitImage;
//
//import java.io.FilePermission;


/**
 * NOTE THIS FILE WAS HEAVILY HACKED DOWN, IT CONTAINS JUST ENOUGH FOR THE ANDROID FORK TO COMPILE
 *
 * Provides a set of functions to be shared among the DataFlavor class and
 * platform-specific data transfer implementations.
 *
 * The concept of "flavors" and "natives" is extended to include "formats",
 * which are the numeric values Win32 and X11 use to express particular data
 * types. Like FlavorMap, which provides getNativesForFlavors(DataFlavor[]) and
 * getFlavorsForNatives(String[]) functions, DataTransferer provides a set
 * of getFormatsFor(Transferable|Flavor|Flavors) and
 * getFlavorsFor(Format|Formats) functions.
 *
 * Also provided are functions for translating a Transferable into a byte
 * array, given a source DataFlavor and a target format, and for translating
 * a byte array or InputStream into an Object, given a source format and
 * a target DataFlavor.
 *
 * @author David Mendenhall
 * @author Danila Sinopalnikov
 *
 * @since 1.3.1
 */
public abstract class DataTransferer {


    /**
     * Cached value of Class.forName("[C");
     */
    public static final Class charArrayClass;

    /**
     * Cached value of Class.forName("[B");
     */
    public static final Class byteArrayClass;


    /**
     * Cache of the platform default encoding as specified in the
     * "file.encoding" system property.
     */
    private static String defaultEncoding;

    /**
     * The singleton DataTransferer instance. It is created during MToolkit
     * or WToolkit initialization.
     */
    private static DataTransferer transferer;

    static {
        Class tCharArrayClass = null, tByteArrayClass = null;
        try {
            tCharArrayClass = Class.forName("[C");
            tByteArrayClass = Class.forName("[B");
        } catch (ClassNotFoundException cannotHappen) {
        }
        charArrayClass = tCharArrayClass;
        byteArrayClass = tByteArrayClass;

        Map tempMap = new HashMap(17);
        tempMap.put("sgml", Boolean.TRUE);
        tempMap.put("xml", Boolean.TRUE);
        tempMap.put("html", Boolean.TRUE);
        tempMap.put("enriched", Boolean.TRUE);
        tempMap.put("richtext", Boolean.TRUE);
        tempMap.put("uri-list", Boolean.TRUE);
        tempMap.put("directory", Boolean.TRUE);
        tempMap.put("css", Boolean.TRUE);
        tempMap.put("calendar", Boolean.TRUE);
        tempMap.put("plain", Boolean.TRUE);
        tempMap.put("rtf", Boolean.FALSE);
        tempMap.put("tab-separated-values", Boolean.FALSE);
        tempMap.put("t140", Boolean.FALSE);
        tempMap.put("rfc822-headers", Boolean.FALSE);
        tempMap.put("parityfec", Boolean.FALSE);
        textMIMESubtypeCharsetSupport = Collections.synchronizedMap(tempMap);
    }


    /**
     * Tracks whether a particular text/* MIME type supports the charset
     * parameter. The Map is initialized with all of the standard MIME types
     * listed in the DataFlavor.selectBestTextFlavor method comment. Additional
     * entries may be added during the life of the JRE for text/<other> types.
     */
    private static final Map textMIMESubtypeCharsetSupport;

    /**
     * Converts an arbitrary text encoding to its canonical name.
     */
    public static String canonicalName(String encoding) {
        if (encoding == null) {
            return null;
        }
        try {
            return Charset.forName(encoding).name();
        } catch (IllegalCharsetNameException icne) {
            return encoding;
        } catch (UnsupportedCharsetException uce) {
            return encoding;
        }
    }


    /**
     * Returns whether this flavor is a text type which supports the
     * 'charset' parameter.
     */
    public static boolean isFlavorCharsetTextType(DataFlavor flavor) {
        // Although stringFlavor doesn't actually support the charset
        // parameter (because its primary MIME type is not "text"), it should
        // be treated as though it does. stringFlavor is semantically
        // equivalent to "text/plain" data.
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return true;
        }

        if (!"text".equals(flavor.getPrimaryType()) ||
                !doesSubtypeSupportCharset(flavor))
        {
            return false;
        }

        Class rep_class = flavor.getRepresentationClass();

        if (flavor.isRepresentationClassReader() ||
                String.class.equals(rep_class) ||
                flavor.isRepresentationClassCharBuffer() ||
                DataTransferer.charArrayClass.equals(rep_class))
        {
            return true;
        }

        if (!(flavor.isRepresentationClassInputStream() ||
                flavor.isRepresentationClassByteBuffer() ||
                DataTransferer.byteArrayClass.equals(rep_class))) {
            return false;
        }

        String charset = flavor.getParameter("charset");

        return (charset != null)
                ? DataTransferer.isEncodingSupported(charset)
                : true; // null equals default encoding which is always supported
    }

    /**
     * Returns whether this flavor is a text type which does not support the
     * 'charset' parameter.
     */
    public static boolean isFlavorNoncharsetTextType(DataFlavor flavor) {
        if (!"text".equals(flavor.getPrimaryType()) ||
                doesSubtypeSupportCharset(flavor))
        {
            return false;
        }

        return (flavor.isRepresentationClassInputStream() ||
                flavor.isRepresentationClassByteBuffer() ||
                DataTransferer.byteArrayClass.
                        equals(flavor.getRepresentationClass()));
    }

    /**
     * Determines whether this JRE can both encode and decode text in the
     * specified encoding.
     */
    public static boolean isEncodingSupported(String encoding) {
        if (encoding == null) {
            return false;
        }
        try {
            return Charset.isSupported(encoding);
        } catch (IllegalCharsetNameException icne) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the given type is a java.rmi.Remote.
     */
    public static boolean isRemote(Class<?> type) {
        return false;
    }

    /**
     * Returns the platform's default character encoding.
     */
    public static String getDefaultTextCharset() {
        if (defaultEncoding != null) {
            return defaultEncoding;
        }
        return defaultEncoding = Charset.defaultCharset().name();
    }


    /**
     * If the specified flavor is a text flavor which supports the "charset"
     * parameter, then this method returns that parameter, or the default
     * charset if no such parameter was specified at construction. For non-
     * text DataFlavors, and for non-charset text flavors, this method returns
     * null.
     */
    public static String getTextCharset(DataFlavor flavor) {
        if (!isFlavorCharsetTextType(flavor)) {
            return null;
        }

        String encoding = flavor.getParameter("charset");

        return (encoding != null) ? encoding : getDefaultTextCharset();
    }



    /**
     * Tests only whether the flavor's MIME type supports the charset
     * parameter. Must only be called for flavors with a primary type of
     * "text".
     */
    public static boolean doesSubtypeSupportCharset(DataFlavor flavor) {
//        if (dtLog.isLoggable(PlatformLogger.FINE)) {
//            if (!"text".equals(flavor.getPrimaryType())) {
//                dtLog.fine("Assertion (\"text\".equals(flavor.getPrimaryType())) failed");
//            }
//        }

        String subType = flavor.getSubType();
        if (subType == null) {
            return false;
        }

        Object support = textMIMESubtypeCharsetSupport.get(subType);

        if (support != null) {
            return (support == Boolean.TRUE);
        }

        boolean ret_val = (flavor.getParameter("charset") != null);
        textMIMESubtypeCharsetSupport.put
                (subType, (ret_val) ? Boolean.TRUE : Boolean.FALSE);
        return ret_val;
    }



    /**
     * A Comparator which includes a helper function for comparing two Objects
     * which are likely to be keys in the specified Map.
     */
    public abstract static class IndexedComparator implements Comparator {

        /**
         * The best Object (e.g., DataFlavor) will be the last in sequence.
         */
        public static final boolean SELECT_BEST = true;

        /**
         * The best Object (e.g., DataFlavor) will be the first in sequence.
         */
        public static final boolean SELECT_WORST = false;

        protected final boolean order;

        public IndexedComparator() {
            this(SELECT_BEST);
        }

        public IndexedComparator(boolean order) {
            this.order = order;
        }

        /**
         * Helper method to compare two objects by their Integer indices in the
         * given map. If the map doesn't contain an entry for either of the
         * objects, the fallback index will be used for the object instead.
         *
         * @param indexMap the map which maps objects into Integer indexes.
         * @param obj1 the first object to be compared.
         * @param obj2 the second object to be compared.
         * @param fallbackIndex the Integer to be used as a fallback index.
         * @return a negative integer, zero, or a positive integer as the
         *             first object is mapped to a less, equal to, or greater
         *             index than the second.
         */
        protected static int compareIndices(Map indexMap,
                                            Object obj1, Object obj2,
                                            Integer fallbackIndex) {
            Integer index1 = (Integer)indexMap.get(obj1);
            Integer index2 = (Integer)indexMap.get(obj2);

            if (index1 == null) {
                index1 = fallbackIndex;
            }
            if (index2 == null) {
                index2 = fallbackIndex;
            }

            return index1.compareTo(index2);
        }

        /**
         * Helper method to compare two objects by their Long indices in the
         * given map. If the map doesn't contain an entry for either of the
         * objects, the fallback index will be used for the object instead.
         *
         * @param indexMap the map which maps objects into Long indexes.
         * @param obj1 the first object to be compared.
         * @param obj2 the second object to be compared.
         * @param fallbackIndex the Long to be used as a fallback index.
         * @return a negative integer, zero, or a positive integer as the
         *             first object is mapped to a less, equal to, or greater
         *             index than the second.
         */
        protected static int compareLongs(Map indexMap,
                                          Object obj1, Object obj2,
                                          Long fallbackIndex) {
            Long index1 = (Long)indexMap.get(obj1);
            Long index2 = (Long)indexMap.get(obj2);

            if (index1 == null) {
                index1 = fallbackIndex;
            }
            if (index2 == null) {
                index2 = fallbackIndex;
            }

            return index1.compareTo(index2);
        }
    }

    /**
     * An IndexedComparator which compares two String charsets. The comparison
     * follows the rules outlined in DataFlavor.selectBestTextFlavor. In order
     * to ensure that non-Unicode, non-ASCII, non-default charsets are sorted
     * in alphabetical order, charsets are not automatically converted to their
     * canonical forms.
     */
    public static class CharsetComparator extends IndexedComparator {
        private static final Map charsets;
        private static String defaultEncoding;

        private static final Integer DEFAULT_CHARSET_INDEX = Integer.valueOf(2);
        private static final Integer OTHER_CHARSET_INDEX = Integer.valueOf(1);
        private static final Integer WORST_CHARSET_INDEX = Integer.valueOf(0);
        private static final Integer UNSUPPORTED_CHARSET_INDEX =
                Integer.valueOf(Integer.MIN_VALUE);

        private static final String UNSUPPORTED_CHARSET = "UNSUPPORTED";

        static {
            HashMap charsetsMap = new HashMap(8, 1.0f);

            // we prefer Unicode charsets
            charsetsMap.put(canonicalName("UTF-16LE"), Integer.valueOf(4));
            charsetsMap.put(canonicalName("UTF-16BE"), Integer.valueOf(5));
            charsetsMap.put(canonicalName("UTF-8"), Integer.valueOf(6));
            charsetsMap.put(canonicalName("UTF-16"), Integer.valueOf(7));

            // US-ASCII is the worst charset supported
            charsetsMap.put(canonicalName("US-ASCII"), WORST_CHARSET_INDEX);

            String defEncoding = DataTransferer.canonicalName
                    (DataTransferer.getDefaultTextCharset());

            if (charsetsMap.get(defaultEncoding) == null) {
                charsetsMap.put(defaultEncoding, DEFAULT_CHARSET_INDEX);
            }
            charsetsMap.put(UNSUPPORTED_CHARSET, UNSUPPORTED_CHARSET_INDEX);

            charsets = Collections.unmodifiableMap(charsetsMap);
        }

        public CharsetComparator() {
            this(SELECT_BEST);
        }

        public CharsetComparator(boolean order) {
            super(order);
        }

        /**
         * Compares two String objects. Returns a negative integer, zero,
         * or a positive integer as the first charset is worse than, equal to,
         * or better than the second.
         *
         * @param obj1 the first charset to be compared
         * @param obj2 the second charset to be compared
         * @return a negative integer, zero, or a positive integer as the
         *         first argument is worse, equal to, or better than the
         *         second.
         * @throws ClassCastException if either of the arguments is not
         *         instance of String
         * @throws NullPointerException if either of the arguments is
         *         <code>null</code>.
         */
        public int compare(Object obj1, Object obj2) {
            String charset1 = null;
            String charset2 = null;
            if (order == SELECT_BEST) {
                charset1 = (String)obj1;
                charset2 = (String)obj2;
            } else {
                charset1 = (String)obj2;
                charset2 = (String)obj1;
            }

            return compareCharsets(charset1, charset2);
        }

        /**
         * Compares charsets. Returns a negative integer, zero, or a positive
         * integer as the first charset is worse than, equal to, or better than
         * the second.
         * <p>
         * Charsets are ordered according to the following rules:
         * <ul>
         * <li>All unsupported charsets are equal.
         * <li>Any unsupported charset is worse than any supported charset.
         * <li>Unicode charsets, such as "UTF-16", "UTF-8", "UTF-16BE" and
         *     "UTF-16LE", are considered best.
         * <li>After them, platform default charset is selected.
         * <li>"US-ASCII" is the worst of supported charsets.
         * <li>For all other supported charsets, the lexicographically less
         *     one is considered the better.
         * </ul>
         *
         * @param charset1 the first charset to be compared
         * @param charset2 the second charset to be compared.
         * @return a negative integer, zero, or a positive integer as the
         *             first argument is worse, equal to, or better than the
         *             second.
         */
        protected int compareCharsets(String charset1, String charset2) {
            charset1 = getEncoding(charset1);
            charset2 = getEncoding(charset2);

            int comp = compareIndices(charsets, charset1, charset2,
                    OTHER_CHARSET_INDEX);

            if (comp == 0) {
                return charset2.compareTo(charset1);
            }

            return comp;
        }

        /**
         * Returns encoding for the specified charset according to the
         * following rules:
         * <ul>
         * <li>If the charset is <code>null</code>, then <code>null</code> will
         *     be returned.
         * <li>Iff the charset specifies an encoding unsupported by this JRE,
         *     <code>UNSUPPORTED_CHARSET</code> will be returned.
         * <li>If the charset specifies an alias name, the corresponding
         *     canonical name will be returned iff the charset is a known
         *     Unicode, ASCII, or default charset.
         * </ul>
         *
         * @param charset the charset.
         * @return an encoding for this charset.
         */
        protected static String getEncoding(String charset) {
            if (charset == null) {
                return null;
            } else if (!DataTransferer.isEncodingSupported(charset)) {
                return UNSUPPORTED_CHARSET;
            } else {
                // Only convert to canonical form if the charset is one
                // of the charsets explicitly listed in the known charsets
                // map. This will happen only for Unicode, ASCII, or default
                // charsets.
                String canonicalName = DataTransferer.canonicalName(charset);
                return (charsets.containsKey(canonicalName))
                        ? canonicalName
                        : charset;
            }
        }
    }

    /**
     * An IndexedComparator which compares two DataFlavors. For text flavors,
     * the comparison follows the rules outlined in
     * DataFlavor.selectBestTextFlavor. For non-text flavors, unknown
     * application MIME types are preferred, followed by known
     * application/x-java-* MIME types. Unknown application types are preferred
     * because if the user provides his own data flavor, it will likely be the
     * most descriptive one. For flavors which are otherwise equal, the
     * flavors' native formats are compared, with greater long values
     * taking precedence.
     */
    public static class DataFlavorComparator extends IndexedComparator {
        protected final Map flavorToFormatMap;

        private final CharsetComparator charsetComparator;

        private static final Map exactTypes;
        private static final Map primaryTypes;
        private static final Map nonTextRepresentations;
        private static final Map textTypes;
        private static final Map decodedTextRepresentations;
        private static final Map encodedTextRepresentations;

        private static final Integer UNKNOWN_OBJECT_LOSES =
                Integer.valueOf(Integer.MIN_VALUE);
        private static final Integer UNKNOWN_OBJECT_WINS =
                Integer.valueOf(Integer.MAX_VALUE);

        private static final Long UNKNOWN_OBJECT_LOSES_L =
                Long.valueOf(Long.MIN_VALUE);
        private static final Long UNKNOWN_OBJECT_WINS_L =
                Long.valueOf(Long.MAX_VALUE);

        static {
            {
                HashMap exactTypesMap = new HashMap(4, 1.0f);

                // application/x-java-* MIME types
                exactTypesMap.put("application/x-java-file-list",
                        Integer.valueOf(0));
                exactTypesMap.put("application/x-java-serialized-object",
                        Integer.valueOf(1));
                exactTypesMap.put("application/x-java-jvm-local-objectref",
                        Integer.valueOf(2));
                exactTypesMap.put("application/x-java-remote-object",
                        Integer.valueOf(3));

                exactTypes = Collections.unmodifiableMap(exactTypesMap);
            }

            {
                HashMap primaryTypesMap = new HashMap(1, 1.0f);

                primaryTypesMap.put("application", Integer.valueOf(0));

                primaryTypes = Collections.unmodifiableMap(primaryTypesMap);
            }

            {
                HashMap nonTextRepresentationsMap = new HashMap(3, 1.0f);

                nonTextRepresentationsMap.put(java.io.InputStream.class,
                        Integer.valueOf(0));
                nonTextRepresentationsMap.put(java.io.Serializable.class,
                        Integer.valueOf(1));

                Class<?> remoteClass = null;
                if (remoteClass != null) {
                    nonTextRepresentationsMap.put(remoteClass,
                            Integer.valueOf(2));
                }

                nonTextRepresentations =
                        Collections.unmodifiableMap(nonTextRepresentationsMap);
            }

            {
                HashMap textTypesMap = new HashMap(16, 1.0f);

                // plain text
                textTypesMap.put("text/plain", Integer.valueOf(0));

                // stringFlavor
                textTypesMap.put("application/x-java-serialized-object",
                        Integer.valueOf(1));

                // misc
                textTypesMap.put("text/calendar", Integer.valueOf(2));
                textTypesMap.put("text/css", Integer.valueOf(3));
                textTypesMap.put("text/directory", Integer.valueOf(4));
                textTypesMap.put("text/parityfec", Integer.valueOf(5));
                textTypesMap.put("text/rfc822-headers", Integer.valueOf(6));
                textTypesMap.put("text/t140", Integer.valueOf(7));
                textTypesMap.put("text/tab-separated-values", Integer.valueOf(8));
                textTypesMap.put("text/uri-list", Integer.valueOf(9));

                // enriched
                textTypesMap.put("text/richtext", Integer.valueOf(10));
                textTypesMap.put("text/enriched", Integer.valueOf(11));
                textTypesMap.put("text/rtf", Integer.valueOf(12));

                // markup
                textTypesMap.put("text/html", Integer.valueOf(13));
                textTypesMap.put("text/xml", Integer.valueOf(14));
                textTypesMap.put("text/sgml", Integer.valueOf(15));

                textTypes = Collections.unmodifiableMap(textTypesMap);
            }

            {
                HashMap decodedTextRepresentationsMap = new HashMap(4, 1.0f);

                decodedTextRepresentationsMap.put
                        (DataTransferer.charArrayClass, Integer.valueOf(0));
                decodedTextRepresentationsMap.put
                        (java.nio.CharBuffer.class, Integer.valueOf(1));
                decodedTextRepresentationsMap.put
                        (java.lang.String.class, Integer.valueOf(2));
                decodedTextRepresentationsMap.put
                        (java.io.Reader.class, Integer.valueOf(3));

                decodedTextRepresentations =
                        Collections.unmodifiableMap(decodedTextRepresentationsMap);
            }

            {
                HashMap encodedTextRepresentationsMap = new HashMap(3, 1.0f);

                encodedTextRepresentationsMap.put
                        (DataTransferer.byteArrayClass, Integer.valueOf(0));
                encodedTextRepresentationsMap.put
                        (java.nio.ByteBuffer.class, Integer.valueOf(1));
                encodedTextRepresentationsMap.put
                        (java.io.InputStream.class, Integer.valueOf(2));

                encodedTextRepresentations =
                        Collections.unmodifiableMap(encodedTextRepresentationsMap);
            }
        }

        public DataFlavorComparator() {
            this(SELECT_BEST);
        }

        public DataFlavorComparator(boolean order) {
            super(order);

            charsetComparator = new CharsetComparator(order);
            flavorToFormatMap = Collections.EMPTY_MAP;
        }

        public DataFlavorComparator(Map map) {
            this(map, SELECT_BEST);
        }

        public DataFlavorComparator(Map map, boolean order) {
            super(order);

            charsetComparator = new CharsetComparator(order);
            HashMap hashMap = new HashMap(map.size());
            hashMap.putAll(map);
            flavorToFormatMap = Collections.unmodifiableMap(hashMap);
        }

        public int compare(Object obj1, Object obj2) {
            DataFlavor flavor1 = null;
            DataFlavor flavor2 = null;
            if (order == SELECT_BEST) {
                flavor1 = (DataFlavor)obj1;
                flavor2 = (DataFlavor)obj2;
            } else {
                flavor1 = (DataFlavor)obj2;
                flavor2 = (DataFlavor)obj1;
            }

            if (flavor1.equals(flavor2)) {
                return 0;
            }

            int comp = 0;

            String primaryType1 = flavor1.getPrimaryType();
            String subType1 = flavor1.getSubType();
            String mimeType1 = primaryType1 + "/" + subType1;
            Class class1 = flavor1.getRepresentationClass();

            String primaryType2 = flavor2.getPrimaryType();
            String subType2 = flavor2.getSubType();
            String mimeType2 = primaryType2 + "/" + subType2;
            Class class2 = flavor2.getRepresentationClass();

            if (flavor1.isFlavorTextType() && flavor2.isFlavorTextType()) {
                // First, compare MIME types
                comp = compareIndices(textTypes, mimeType1, mimeType2,
                        UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }

                // Only need to test one flavor because they both have the
                // same MIME type. Also don't need to worry about accidentally
                // passing stringFlavor because either
                //   1. Both flavors are stringFlavor, in which case the
                //      equality test at the top of the function succeeded.
                //   2. Only one flavor is stringFlavor, in which case the MIME
                //      type comparison returned a non-zero value.
                if (doesSubtypeSupportCharset(flavor1)) {
                    // Next, prefer the decoded text representations of Reader,
                    // String, CharBuffer, and [C, in that order.
                    comp = compareIndices(decodedTextRepresentations, class1,
                            class2, UNKNOWN_OBJECT_LOSES);
                    if (comp != 0) {
                        return comp;
                    }

                    // Next, compare charsets
                    comp = charsetComparator.compareCharsets
                            (DataTransferer.getTextCharset(flavor1),
                                    DataTransferer.getTextCharset(flavor2));
                    if (comp != 0) {
                        return comp;
                    }
                }

                // Finally, prefer the encoded text representations of
                // InputStream, ByteBuffer, and [B, in that order.
                comp = compareIndices(encodedTextRepresentations, class1,
                        class2, UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }
            } else {
                // First, prefer application types.
                comp = compareIndices(primaryTypes, primaryType1, primaryType2,
                        UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }

                // Next, look for application/x-java-* types. Prefer unknown
                // MIME types because if the user provides his own data flavor,
                // it will likely be the most descriptive one.
                comp = compareIndices(exactTypes, mimeType1, mimeType2,
                        UNKNOWN_OBJECT_WINS);
                if (comp != 0) {
                    return comp;
                }

                // Finally, prefer the representation classes of Remote,
                // Serializable, and InputStream, in that order.
                comp = compareIndices(nonTextRepresentations, class1, class2,
                        UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }
            }

            // As a last resort, take the DataFlavor with the greater integer
            // format.
            return compareLongs(flavorToFormatMap, flavor1, flavor2,
                    UNKNOWN_OBJECT_LOSES_L);
        }
    }


}
