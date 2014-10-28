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

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import org.mitre.cpe.language.x20.FactRefType;
import org.mitre.cpe.language.x20.LogicalTestType;
import org.mitre.cpe.language.x20.OperatorEnumeration;
import org.mitre.cpe.language.x20.PlatformType;


public class CPELanguage {
    private final PlatformType platform;

    public CPELanguage(final PlatformType platform) {
        this.platform = platform;
    }

    public String getId() { return platform.getId(); }

    public boolean match(final Collection<CPEName> names) throws URISyntaxException {
        for (CPEName name : names) {
            if (matchLogicalTest(platform.getLogicalTest(),name)) {
                return true;
            }
        }
        return false;
    }

    public boolean match(final CPEName name) throws URISyntaxException {
        return matchLogicalTest(platform.getLogicalTest(),name);
    }

    public CPEResolver.Status evaluate(final Set<CPEName> foundCPEs)
             throws URISyntaxException {
        return evaluateLogicalTest(platform.getLogicalTest(),foundCPEs);
    }

    private CPEResolver.Status evaluateLogicalTest(
            final LogicalTestType logicalTest,
            final Set<CPEName> foundCPEs) throws URISyntaxException {
        boolean negate = logicalTest.getNegate();
        OperatorEnumeration.Enum operator = logicalTest.getOperator();

        CPEResolver.Status retval = null;
        main: for (LogicalTestType test : logicalTest.getLogicalTestList()) {
            CPEResolver.Status result = evaluateLogicalTest(test,foundCPEs);
            switch (operator.intValue()) {
                case OperatorEnumeration.INT_AND: {
                    if (result != CPEResolver.Status.PASS) {
                        retval = result;
                        break main;
                    } else {
                        retval = result;
                    }
                    break;
                }
                case OperatorEnumeration.INT_OR: {
                    if (result == CPEResolver.Status.PASS) {
                        retval = result;
                        break main;
                    } else if (result == CPEResolver.Status.FAIL) {
                        retval = result; // definiative fail result
                    } else { // result is either error or unknown
                        if (retval != CPEResolver.Status.FAIL
                                && result == CPEResolver.Status.ERROR) {
                            // error trumps unknown
                            retval = result;
                        }
                    }
                }
            }
        }

        if (operator.intValue() == OperatorEnumeration.INT_AND
                || retval != CPEResolver.Status.PASS) { // OR and no definative result
            main: for (FactRefType factRef : logicalTest.getFactRefList()) {
                final CPEName fact = new CPEName(factRef.getName());
                boolean result = foundCPEs.contains(fact);
                switch (operator.intValue()) {
                    case OperatorEnumeration.INT_AND: {
                        if (result == false) {
                            retval = CPEResolver.Status.FAIL;
                            break main;
                        } else {
                            retval = CPEResolver.Status.PASS;
                        }
                        break;
                    }
                    case OperatorEnumeration.INT_OR: {
                        if (result == false) {
                            retval = CPEResolver.Status.FAIL;
                        } else { // retval == true
                            retval = CPEResolver.Status.PASS;
                            break main;
                        }
                    }
                }
            }
        }

        if (retval == null) retval = CPEResolver.Status.UNKNOWN;

        if (negate) {
            if (retval == CPEResolver.Status.PASS) {
                retval = CPEResolver.Status.FAIL;
            } else if (retval == CPEResolver.Status.FAIL) {
                retval = CPEResolver.Status.PASS;
            }
        }
        return retval;
    }

    private boolean matchLogicalTest(
            final LogicalTestType logicalTest,
            final CPEName name) throws URISyntaxException {
        boolean negate = logicalTest.getNegate();
        OperatorEnumeration.Enum operator = logicalTest.getOperator();

        Boolean retval = null;
        main: for (LogicalTestType test : logicalTest.getLogicalTestList()) {
            boolean result = matchLogicalTest(test,name);
            switch (operator.intValue()) {
                case OperatorEnumeration.INT_AND: {
                    if (result == false) {
                        retval = false;
                        break main;
                    } else {
                        retval = true;
                    }
                    break;
                }
                case OperatorEnumeration.INT_OR: {
                    if (result == false) {
                        retval = false;
                    } else { // retval == true
                        retval = true;
                        break main;
                    }
                }
            }
        }

        if (operator.intValue() == OperatorEnumeration.INT_AND
                || retval != true) { // OR and no definative result
            main: for (FactRefType factRef : logicalTest.getFactRefList()) {
                CPEName fact = new CPEName(factRef.getName());
                boolean result = fact.nameMatch(name);
                switch (operator.intValue()) {
                    case OperatorEnumeration.INT_AND: {
                        if (result == false) {
                            retval = false;
                            break main;
                        } else {
                            retval = true;
                        }
                        break;
                    }
                    case OperatorEnumeration.INT_OR: {
                        if (result == false) {
                            retval = false;
                        } else { // retval == true
                            retval = true;
                            break main;
                        }
                    }
                }
            }
        }

        if (retval == null) {
            throw(new UnsupportedOperationException("platform has a logical-test node without a child"));
        }

        if (negate) return !retval;
        return retval;
    }
}
