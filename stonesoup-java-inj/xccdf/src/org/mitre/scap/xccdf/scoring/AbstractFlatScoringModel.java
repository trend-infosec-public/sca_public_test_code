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



package org.mitre.scap.xccdf.scoring;

import gov.nist.checklists.xccdf.x11.ModelDocument;
import gov.nist.checklists.xccdf.x11.ResultEnumType;
import gov.nist.checklists.xccdf.x11.RuleResultType;
import gov.nist.checklists.xccdf.x11.RuleType;
import gov.nist.checklists.xccdf.x11.TestResultType;
import org.mitre.scap.xccdf.XCCDFInterpreter;
import java.math.BigDecimal;
import java.math.MathContext;


public abstract class AbstractFlatScoringModel extends AbstractScoringModel {
    public AbstractFlatScoringModel(final String system) {
        super(system);
    }

    @SuppressWarnings("fallthrough")
	public final Score score(
            final XCCDFInterpreter interpreter,
            final TestResultType results,
            final ModelDocument.Model model) {

        // Score.Init - Initialize both the score s and the maximum score m to 0.0.
        BigDecimal s = BigDecimal.ZERO.setScale(1);
        BigDecimal m = BigDecimal.ZERO.setScale(1);

        // Score.Rules - For each element e in V where e.p is not a member of
        // the set {notapplicable, notchecked, informational, notselected}:
        // - add the weight of rule e.r to m
        // - if the value e.p equals ‘pass’ or ‘fixed’, add the weight of the rule
        //   e.r to s.
        for (RuleResultType ruleResult : results.getRuleResultList()) {
            BigDecimal weight = getWeight(interpreter.lookupRule(ruleResult.getIdref()));
            switch (ruleResult.getResult().intValue()) {
                case ResultEnumType.INT_NOTAPPLICABLE:
                case ResultEnumType.INT_NOTCHECKED:
                case ResultEnumType.INT_INFORMATIONAL:
                case ResultEnumType.INT_NOTSELECTED:
                    continue;
                case ResultEnumType.INT_PASS:
                case ResultEnumType.INT_FIXED:
                    s = s.add(weight,MathContext.UNLIMITED);
                    // Also run the default statement
                default:
                    m = m.add(weight,MathContext.UNLIMITED);
            }
        }

        final BigDecimal finalS = s;
        final BigDecimal finalM = m;
        return new Score() {

            public BigDecimal getMaximumScore() {
                return finalM;
            }

            public BigDecimal getScore() {
                return finalS;
            }
            
        };
    }

    protected abstract BigDecimal getWeight(final RuleType rule);
}
