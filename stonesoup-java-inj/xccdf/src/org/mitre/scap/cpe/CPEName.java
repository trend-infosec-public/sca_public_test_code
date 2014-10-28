/*****************************************************************************
 *  License Agreement
 *
 *  Copyright (c) 2011, The MITRE Corporation
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright notice, this list
 *        of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright notice, this
 *        list of conditions and the following disclaimer in the documentation and/or other
 *        materials provided with the distribution.
 *      * Neither the name of The MITRE Corporation nor the names of its contributors may be
 *        used to endorse or promote products derived from this software without specific
 *        prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 *  SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 *  OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  This code was originally developed at the National Institute of Standards and Technology,
 *  a government agency under the United States Department of Commerce, and was subsequently
 *  modified at The MITRE Corporation. Subsequently, the resulting program is not an official
 *  NIST version of the original source code. Pursuant to Title 17, Section 105 of the United
 *  States Code the original NIST code and any accompanying documentation created at NIST are
 *  not subject to copyright protection and are in the public domain.
 *
 *****************************************************************************/


package org.mitre.scap.cpe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class CPEName {
    /** The CPE URI scheme name */

    public static final String SCHEME = "cpe";

    /** Value for the first component of a hardware CPE Name */
    public static final String PART_HARDWARE = "h";

    /** Value for the first component of an operating system CPE Name */
    public static final String PART_OS = "o";

    /** Maximum number of components in a CPE Name */
    protected static final int MAX_COMPONENTS = 7;

    /** Value for the first component of an application CPE Name */
    public static final String PART_APPLICATION = "a";

    /* object state - all of these are serializable */
    private URI uri;
    private String[] components;

    /* cached state to speed up hashCode, not serialized */
    private transient int hash_cache = 0;

    /**
     * Create a CPEName from a string.
     */
    public CPEName(String name) throws URISyntaxException {
        uri = new URI(name);
        if (uri == null || uri.getScheme() == null || !(uri.getScheme().equals(SCHEME))) {
            throw new URISyntaxException(name, "Bad CPE scheme, must be '"+SCHEME+"': "+name);
        }
        String path;
        path = uri.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        components = path.split(":");
        if (components.length > MAX_COMPONENTS) {
            throw new URISyntaxException(name, "Bad CPE URI: maximum number of name components exceeded");
        }
    }

    /**
     * Return a string representation of the CPE Name
     */
    @Override
    public String toString() {
        return uri.toString();
    }

    /**
     * Return the hashcode.
     */
    @Override
    public int hashCode() {
        if (hash_cache == 0) {
            hash_cache = uri.toString().toLowerCase().hashCode();
        }
        return hash_cache;
    }

    /**
     * Return true if this CPEName equals another.  Note that
     * this is different from matching!
     */
    @Override
    public boolean equals(Object o) {
        CPEName that;
        boolean answer = false;
        if (o instanceof CPEName) {
            that = (CPEName) o;
            if (this.components.length == that.components.length) {
                answer = true;
                for (int i = 0; i < components.length; i++) {
                    if (!(components[i].equalsIgnoreCase(that.components[i]))) {
                        answer = false;
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Return the number of components in this CPE Name
     * (including empty components).
     */
    public int getLength() {
        return components.length;
    }

    /**
     * Return a specific component, or null if that
     * component was not defined.  The first component
     * is number 0.  If the input is invalid, then
     * ArrayIndexOutOfBoundsException will be thrown.
     */
    public String getComponent(int i) {
        return components[i];
    }

    /**
     * Return true iff this CPE Name matches one of a
     * collection of known CPE Names.  (This implements
     * the <b>CPE_Name_Match</b> algorithm from the
     * CPE 2.0 specification section 7.)
     * Any members of the collection that are not CPEName
     * objects will be ignored.
     */
    public boolean nameMatch(Collection<CPEName> k) {
        boolean retval = false;
        for (CPEName n : k) {
            boolean result = nameMatch(n);
            if (result) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public boolean nameMatch(final CPEName n) {
        // if length(n) >= length(this) then we may have a match
        if (n.getLength() >= getLength()) {
            boolean r = false;
            // check each component of n and this
            for (int i = 0; i < getLength(); i++) {
                // components equal, or this component is empty
                if (getComponent(i).equalsIgnoreCase(n.getComponent(i)) || getComponent(i).equals("")) {
                    r = true;
                } else {
                    r = false;
                    break;
                }
            }
            // if r is true, then we have a match
            if (r) {
                return true;
            }
        }
        return false;
    }


    /**
     * Main for testing.  Takes the first argument
     * and treats it as a CPE Name.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java CPEName cpe-name [cpe-name ..]");
            System.exit(1);
        }

        try {
            int i;
            String x = args[0];
            CPEName cpe = new CPEName(x);
            System.out.println("Name '" + cpe.toString() + "', length is " +
                    cpe.getLength());
            for (i = 0; i < cpe.getLength(); i++) {
                System.out.println("\tcomp[" + i + "] = '" +
                        cpe.getComponent(i) + "'");
            }

            if (args.length == 2) {
                String x2 = args[1];
                CPEName cpe2 = new CPEName(x2);
                if (cpe.equals(cpe2)) {
                    System.out.println(cpe + " = " + cpe2);
                } else {
                    System.out.println(cpe + " != " + cpe2);
                }
            }

            if (args.length > 2) {
                Set<CPEName> k = new HashSet<CPEName>();
                for (int j = 1; j < args.length; j++) {
                    CPEName cpe3 = new CPEName(args[j]);
                    System.out.println("Member of k: " + cpe3);
                    k.add(cpe3);
                }
                System.out.println("Set k has " + k.size() + " members.");
                if (cpe.nameMatch(k)) {
                    System.out.println(cpe + " matches k");
                } else {
                    System.out.println(cpe + " does not match k");
                }
            }
        } catch (URISyntaxException ux) {
            System.err.println("!! " + ux);
            ux.printStackTrace();
        }
    }
}
