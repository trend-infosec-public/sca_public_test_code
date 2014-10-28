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



package org.mitre.scap.xccdf;

import gov.nist.checklists.xccdf.x11.*;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;


public class XCCDFVisitor {

    public static void visit(final BenchmarkDocument document, final XCCDFVisitorHandler handler) {
        final BenchmarkDocument.Benchmark benchmark = document.getBenchmark();

        if (benchmark == null) {
            return;
        }
        handler.enterBenchmark(benchmark);
        handler.visitBenchmark(benchmark);

        for (ProfileType profile : benchmark.getProfileList()) {
            handler.visitProfile(profile, benchmark);
        }

        for (ValueType value : benchmark.getValueList()) {
            handler.visitItem(value, benchmark, null);
            handler.visitValue(value, benchmark, null);
        }

        if ((benchmark.sizeOfRuleArray() + benchmark.sizeOfGroupArray()) > 0) {
            XmlCursor cursor = benchmark.newCursor();
            if (!cursor.toFirstChild()) {
                // TODO: log error
            }

            do {
                XmlObject xmlObject = cursor.getObject();
                if (SelectableItemType.type.isAssignableFrom(xmlObject.schemaType())) {
                    SelectableItemType item = (SelectableItemType) xmlObject;
                    handler.visitItem(item, benchmark, null);
                    handler.visitSelectableItem(item, benchmark, null);

                    if (item instanceof GroupType) {
                        handler.enterGroup((GroupType) item, benchmark, null);
                        if (handler.visitGroup((GroupType) item, benchmark, null)) {
                            visitGroup(benchmark, handler, (GroupType) item);
                        }
                        handler.exitGroup((GroupType) item, benchmark, null);
                    } else if (item instanceof RuleType) {
                        handler.visitRule((RuleType) item, benchmark, null);
                    }
                }
            } while (cursor.toNextSibling());
        }
        handler.exitBenchmark(benchmark);
    }



    
    public static void visitGroup(
            final BenchmarkDocument.Benchmark benchmark,
            final XCCDFVisitorHandler handler,
            final GroupType group) {

        for (ValueType value : group.getValueList()) {
            handler.visitItem(value, benchmark, group);
            handler.visitValue(value, benchmark, group);
        }

        if ((group.sizeOfRuleArray() + group.sizeOfGroupArray()) > 0) {
            XmlCursor cursor = group.newCursor();
            if (!cursor.toFirstChild()) {
                // TODO: log error
            }

            do {
                XmlObject xmlObject = cursor.getObject();
                if (SelectableItemType.type.isAssignableFrom(xmlObject.schemaType())) {
                    SelectableItemType item = (SelectableItemType) xmlObject;
                    handler.visitItem(item, benchmark, group);
                    handler.visitSelectableItem(item, benchmark, group);

                    if (item instanceof GroupType) {
                        handler.enterGroup((GroupType) item, benchmark, group);
                        if (handler.visitGroup((GroupType) item, benchmark, group)) {
                            visitGroup(benchmark, handler, (GroupType) item);
                        }
                        handler.exitGroup((GroupType) item, benchmark, group);
                    } else if (item instanceof RuleType) {
                        handler.visitRule((RuleType) item, benchmark, group);
                    }
                }
            } while (cursor.toNextSibling());
        }
    }
}