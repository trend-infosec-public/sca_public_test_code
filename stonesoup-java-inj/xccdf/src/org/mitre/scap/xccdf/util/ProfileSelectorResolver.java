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





package org.mitre.scap.xccdf.util;

import gov.nist.checklists.xccdf.x11.ProfileRefineRuleType;
import gov.nist.checklists.xccdf.x11.ProfileRefineValueType;
import gov.nist.checklists.xccdf.x11.ProfileSelectType;
import gov.nist.checklists.xccdf.x11.ProfileSetValueType;
import gov.nist.checklists.xccdf.x11.ProfileType;
import gov.nist.checklists.xccdf.x11.SignatureType;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlCursor;


public class ProfileSelectorResolver {
    public static final QNameSet selectorQNameSet;

    private final ProfileType profile;
    private final ProfileType extendedProfile;

    static {
        QName[] qnames = {
            ProfileSelectType.type.getName(),
            ProfileRefineRuleType.type.getName(),
            ProfileSetValueType.type.getName(),
            ProfileRefineValueType.type.getName()
        };
        selectorQNameSet = QNameSet.forArray(qnames);
    }

    public ProfileSelectorResolver(
            final ProfileType profile,
            final ProfileType extendedProfile) {
        this.profile = profile;
        this.extendedProfile = extendedProfile;
    }

    public void resolve() {
        // Prepend extended selectors, if they exist
//        extendedProfile.selectChildren(arg0);

        // Prepend the selectors to the extending profile
        // Find the position of the first selector
        XmlCursor extendingCursor = profile.newCursor();
        extendingCursor.toFirstChild();
        do {
            QName qname = extendingCursor.getObject().schemaType().getName();
            if (selectorQNameSet.contains(qname)
                || qname.equals(SignatureType.type.getName())) {
                break;
            }
        } while (extendingCursor.toNextSibling());

        extendingCursor.insertComment("Begin extended selectors from profile: "+extendedProfile.getId());

        XmlCursor extendedCursor = extendedProfile.newCursor();

        if (!extendedCursor.toFirstChild()) {
            return;
        }

        // Get the selectors from the extended profile
        do {
            if (selectorQNameSet.contains(extendedCursor.getObject().schemaType().getName())) {
                extendedCursor.copyXml(extendingCursor);
            }
        } while (extendedCursor.toNextSibling());

        extendingCursor.insertComment("End extended selectors from profile: "+extendedProfile.getId());
/*
            if (extendedProfile.selectOrSetValueOrRefineValue == null
                    || extendedProfile.selectOrSetValueOrRefineValue.size() == 0) {
                // do nothing, nothing to extend
            } else if (selectOrSetValueOrRefineValue == null
                    || selectOrSetValueOrRefineValue.size() == 0) {
                // Use the extended list of selectors
                selectOrSetValueOrRefineValue = extendedProfile.selectOrSetValueOrRefineValue;
            } else {
                // Prepend the extended list of selectors
                List<Object> newList
                        = new ArrayList<Object>(selectOrSetValueOrRefineValue.size()
                            + extendedProfile.selectOrSetValueOrRefineValue.size());
                newList.addAll(extendedProfile.selectOrSetValueOrRefineValue);
                newList.addAll(selectOrSetValueOrRefineValue);
                selectOrSetValueOrRefineValue = newList;
            }
*/
    }
}
