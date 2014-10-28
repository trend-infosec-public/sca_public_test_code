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

import gov.nist.checklists.xccdf.x11.CheckContentRefType;
import org.mitre.scap.xccdf.check.Check;
import org.mitre.scap.xccdf.check.CheckSystem;
import org.mitre.scap.xccdf.check.CheckSystemRegistry;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mitre.cpe.dictionary.x20.CpeItemDocument;
import org.mitre.cpe.dictionary.x20.CheckType;


public class CPE {
    private final CPEName cpeName;
    private final CPEName deprecatedBy;
    private final boolean deprecated;
    private final List<Check> checks = new ArrayList<Check>();

    public CPE(
            final CpeItemDocument.CpeItem cpeItem,
            final CheckHelper helper) throws URISyntaxException {
        cpeName = new CPEName(cpeItem.getName());
        deprecated = (cpeItem.isSetDeprecated() ? cpeItem.getDeprecated() : false);
        deprecatedBy = (cpeItem.isSetDeprecatedBy() ? new CPEName(cpeItem.getDeprecatedBy()) : null);

        Map<String,List<CheckContentRefType>> systemToRefMap = new HashMap<String,List<CheckContentRefType>>();
        for (CheckType checkType : cpeItem.getCheckList()) {
            String href = (checkType.isSetHref() ? checkType.getHref() : null);
            String name = checkType.getStringValue();
            String system = checkType.getSystem();

            href = helper.resolveHref(href);

            // Lookup the check system
            CheckSystemRegistry.lookupCheckSystem(system);

            CheckContentRefType ref = CheckContentRefType.Factory.newInstance();
            ref.setHref(href);
            ref.setName(name);
            List<CheckContentRefType> refList = systemToRefMap.get(system);
            if (refList == null) {
                refList = new ArrayList<CheckContentRefType>();
                systemToRefMap.put(system, refList);
            }
            refList.add(ref);
        }

        for (Map.Entry<String,List<CheckContentRefType>> entry : systemToRefMap.entrySet()) {
            String system = entry.getKey();
            List<CheckContentRefType> refList = entry.getValue();
            CheckSystem checkSystem = CheckSystemRegistry.lookupCheckSystem(system);
            Check check = checkSystem.newCheck(refList, null);
            checks.add(check);
        }
    }

    public CPEName getCpeName() {
        return cpeName;
    }

    public CPEName getDeprecatedBy() {
        return deprecatedBy;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public List<Check> getChecks() {
        return checks;
    }
}
