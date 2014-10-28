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

import gov.nist.checklists.xccdf.x11.BenchmarkDocument;
import gov.nist.checklists.xccdf.x11.GroupType;
import gov.nist.checklists.xccdf.x11.ResultEnumType;
import gov.nist.checklists.xccdf.x11.RuleResultType;
import gov.nist.checklists.xccdf.x11.RuleType;
import gov.nist.checklists.xccdf.x11.SelectableItemType;
import gov.nist.checklists.xccdf.x11.TestResultType;
import org.mitre.scap.xccdf.XCCDFInterpreter;
import org.mitre.scap.xccdf.XCCDFVisitorHandler;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

class DefaultScoringXCCDFVisitorHandler extends XCCDFVisitorHandler {

    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100.0");
    private final Map<SelectableItemType, Node> itemToNodeMap = new HashMap<SelectableItemType, Node>();
    private final Map<String, RuleResultType> ruleIdToResultMap = new HashMap<String, RuleResultType>();
    private final Stack<SelectableItemType> nodeStack = new Stack<SelectableItemType>();

    protected DefaultScoringXCCDFVisitorHandler(final XCCDFInterpreter interpreter, final TestResultType results) {
        super();
        for (RuleResultType ruleResult : results.getRuleResultList()) {
            ruleIdToResultMap.put(ruleResult.getIdref(), ruleResult);
        }
    }

    public String getPadding() {
        int count = nodeStack.size();
        StringBuilder retval = new StringBuilder();
        for (int i=0;i<count*2;i++) {
            retval.append(' ');
        }
        return retval.toString();
    }

    public Node getBenchmarkNode() {
        return itemToNodeMap.get(null);
    }

    protected BigDecimal getWeight(final SelectableItemType item) {
        return item.isSetWeight() ? item.getWeight() : BigDecimal.ONE;
    }

    @Override
    public void visitRule(final RuleType rule, final BenchmarkDocument.Benchmark benchmark, final GroupType parent) {
//        nodeStack.push(rule);
        boolean process;
        ResultEnumType.Enum result = ruleIdToResultMap.get(rule.getId()).getResult();
        switch (result.intValue()) {
            case ResultEnumType.INT_NOTAPPLICABLE:
            case ResultEnumType.INT_NOTCHECKED:
            case ResultEnumType.INT_INFORMATIONAL:
            case ResultEnumType.INT_NOTSELECTED:
                process = false;
                break;
            default:
                process = true;
        }
        if (process) {
            // Score.Rule - If the node is a Rule, then assign a count of 1, and
            //      if the test result is ‘pass’, assign the node a score of 100,
            //      otherwise assign a score of 0.  
            // Score.Group.Recurse - For each selected child of this Group or
            //      Benchmark, do the following:  (1) compute the count and
            //      weighted score for the child using this algorithm, (2) if
            //      the child’s count value is not 0, then add the child’s
            //      weighted score to this node’s score s, add 1 to this node’s
            //      count, and add the child’s weight value to the accumulator a.
            BigDecimal weight = getWeight(rule);
            Node node = itemToNodeMap.get(parent);
            node.setCount(node.getCount() + 1);
            if (result == ResultEnumType.PASS
                    || result == ResultEnumType.FIXED) {
//                System.out.println(getPadding()+"rule: score=100, weight="+weight.toString());
                node.setScore(node.getScore().add(ONE_HUNDRED.multiply(weight, MathContext.UNLIMITED), MathContext.UNLIMITED));
            } else {
//                System.out.println(getPadding()+"rule: score=0, weight="+weight.toString());
            }
            node.setAccumulator(node.getAccumulator().add(weight, MathContext.UNLIMITED));
//            System.out.println(getPadding()+"sum: score="+node.getScore()+", count="+node.getCount()+", accumulator="+node.getAccumulator());
        }
//        nodeStack.pop();
    }

    @Override
    public void enterBenchmark(final BenchmarkDocument.Benchmark benchmark) {
        // Score.Group.Init - If the node is a Group or the Benchmark, assign a
        //      count of 0, a score s of 0.0, and an accumulator a of 0.0.
        itemToNodeMap.put(null, new Node(0, BigDecimal.ZERO.setScale(1), BigDecimal.ZERO.setScale(1)));
    }

    @Override
    public void exitBenchmark(final BenchmarkDocument.Benchmark benchmark) {
//        System.out.println(getPadding()+"bm: score="+node.getScore()+", count="+node.getCount()+", accumulator="+node.getAccumulator());
    }

    @Override
    public void enterGroup(final GroupType group, final BenchmarkDocument.Benchmark benchmark, final GroupType parent) {
        // Score.Group.Init - If the node is a Group or the Benchmark, assign a
        //      count of 0, a score s of 0.0, and an accumulator a of 0.0.
//        nodeStack.push(group);
        itemToNodeMap.put(group, new Node(0, BigDecimal.ZERO.setScale(1), BigDecimal.ZERO.setScale(1)));
    }

    @Override
    public void exitGroup(
            final GroupType group,
            final BenchmarkDocument.Benchmark benchmark,
            final GroupType parent) {
        BigDecimal weight = getWeight(group);
        // Score.Group.Recurse - For each selected child of this Group or
        //      Benchmark, do the following:  (1) compute the count and weighted
        //      score for the child using this algorithm, (2) if the child’s
        //      count value is not 0, then add the child’s weighted score to
        //      this node’s score s, add 1 to this node’s count, and add the
        //      child’s weight value to the accumulator a.
        Node thisNode = itemToNodeMap.get(group);
//        System.out.println(getPadding()+"curr: score="+thisNode.getScore()+",count="+thisNode.getCount()+", accumulator="+thisNode.getAccumulator()+", weight="+weight);
        if (thisNode.getCount() > 0) {
            // Score.Group.Normalize - Normalize this node’s score:
            //      compute  s  =  s / a.
            BigDecimal score = thisNode.getScore().divide(thisNode.getAccumulator(),RoundingMode.HALF_UP);

            // Score.Weight - Assign the node a weighted score equal to the product
            //      of its score and its weight.
            BigDecimal weightedScore = score.multiply(weight, MathContext.UNLIMITED);
//            System.out.println(getPadding()+"group: score="+score+", weightedScore="+weightedScore);

            Node node = itemToNodeMap.get(parent);
            node.setCount(node.getCount() + 1);
            node.setScore(node.getScore().add(weightedScore, MathContext.UNLIMITED));
            node.setAccumulator(node.getAccumulator().add(weight, MathContext.UNLIMITED));
//            System.out.println(getPadding()+"sum: score="+node.getScore()+", count="+node.getCount()+", accumulator="+node.getAccumulator());
        }
//        nodeStack.pop();
    }

    public class Node {

        private int count;
        private BigDecimal score;
        private BigDecimal accumulator;

        public Node(
                final int count,
                final BigDecimal score) {
            this.count = count;
            this.score = score;
            this.accumulator = null;
        }

        public Node(
                final int count,
                final BigDecimal score,
                final BigDecimal accumulator) {
            this.count = count;
            this.score = score;
            this.accumulator = accumulator;
        }

        public int getCount() {
            return count;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(final BigDecimal score) {
            this.score = score;
        }

        public BigDecimal getAccumulator() {
            return accumulator;
        }

        public void setAccumulator(final BigDecimal accumulator) {
            this.accumulator = accumulator;
        }
    }
}
