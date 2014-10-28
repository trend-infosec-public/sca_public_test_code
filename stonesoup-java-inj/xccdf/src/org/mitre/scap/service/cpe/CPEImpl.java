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

package org.mitre.scap.service.cpe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;


class CPEImpl implements CPE {
    /** The CPE URI scheme name */
    public static final String SCHEME = "cpe";

    /** Value for the first component of a hardware CPE Name */
    public static final String PART_HARDWARE = "h";

    /** Value for the first component of an operating system CPE Name */
    public static final String PART_OS = "o";

    /** Value for the first component of an application CPE Name */
    public static final String PART_APPLICATION = "a";

    /** Maximum number of components in a CPE Name */
    protected static final int MAX_COMPONENTS = 7;

    /* object state - all of these are serializable */
    private URI uri;
    private String[] components;

    /* cached state to speed up hashCode, not serialized */
    private transient int hash_cache = 0;

    /**
     * Create a CPEName from a string.
     */
    public CPEImpl(String name) throws URISyntaxException {
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

    public String getName() {
        return toString();
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
        boolean retval = false;
        if (o instanceof CPEImpl) {
            CPEImpl that = (CPEImpl) o;
            if (this.components.length == that.components.length) {
                retval = true;
                for (int i = 0; i < components.length; i++) {
                    if (!(components[i].equalsIgnoreCase(that.components[i]))) {
                        retval = false;
                    }
                }
            }
        }
        return retval;
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
    public boolean matches(Collection<CPE> c) {
        boolean retval = false;
        for (CPE cpe : c) {
            if (matches(cpe)) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public boolean matches(final CPE cpe) {
        // if length(cpe) >= length(this) then we may have a match
        if (cpe.getLength() < getLength()) {
            return false;
        }

        boolean retval = false;
        // check each component of n and this
        for (int i = 0; i < getLength(); i++) {
            // components equal, or this component is empty
            if (getComponent(i).equalsIgnoreCase(cpe.getComponent(i))
                    || getComponent(i).length() == 0
                    || cpe.getComponent(i).length() == 0) {
                retval = true;
            } else {
                retval = false;
                break;
            }
        }

        return retval;
    }
}
