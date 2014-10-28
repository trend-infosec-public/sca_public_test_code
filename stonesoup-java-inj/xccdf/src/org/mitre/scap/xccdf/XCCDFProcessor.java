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

import org.mitre.scap.xccdf.properties.BuildProperties;
import org.mitre.ByteUtil;
import org.mitre.scap.cpe.CPEResolver;
import org.mitre.scap.xccdf.scoring.ScoringModel;
import gov.nist.checklists.xccdf.x11.BenchmarkDocument;
import gov.nist.checklists.xccdf.x11.BenchmarkDocument.Benchmark;
import gov.nist.checklists.xccdf.x11.CheckContentRefType;
import gov.nist.checklists.xccdf.x11.CheckExportType;
import gov.nist.checklists.xccdf.x11.CheckType;
import gov.nist.checklists.xccdf.x11.ComplexCheckType;
import gov.nist.checklists.xccdf.x11.FactType;
import gov.nist.checklists.xccdf.x11.GroupType;
import gov.nist.checklists.xccdf.x11.IdentType;
import gov.nist.checklists.xccdf.x11.IdrefListType;
import gov.nist.checklists.xccdf.x11.IdrefType;
import gov.nist.checklists.xccdf.x11.InstanceResultType;
import gov.nist.checklists.xccdf.x11.MessageType;
import gov.nist.checklists.xccdf.x11.ModelDocument;
import gov.nist.checklists.xccdf.x11.MsgSevEnumType;
import gov.nist.checklists.xccdf.x11.OverrideableURIidrefType;
import gov.nist.checklists.xccdf.x11.ProfileSetValueType;
import gov.nist.checklists.xccdf.x11.ResultEnumType;
import gov.nist.checklists.xccdf.x11.RoleEnumType;
import gov.nist.checklists.xccdf.x11.RuleResultType;
import gov.nist.checklists.xccdf.x11.RuleType;
import gov.nist.checklists.xccdf.x11.ScoreType;
import gov.nist.checklists.xccdf.x11.SelectableItemType;
import gov.nist.checklists.xccdf.x11.TargetFactsType;
import gov.nist.checklists.xccdf.x11.TestResultType;
import gov.nist.checklists.xccdf.x11.TextType;
import gov.nist.checklists.xccdf.x11.ValueType;
import gov.nist.checklists.xccdf.x11.ValueTypeType;
import org.mitre.scap.xccdf.check.Check;
import org.mitre.scap.xccdf.check.CheckInterpreter;
import org.mitre.scap.xccdf.check.CheckResult;
import org.mitre.scap.xccdf.check.CheckSystem;
import org.mitre.scap.xccdf.check.CheckSystemRegistry;
import org.mitre.scap.xccdf.check.complexcheck.ComplexCheck;
import org.mitre.scap.xccdf.check.complexcheck.ComplexCheckException;
import org.mitre.scap.xccdf.check.complexcheck.ComplexCheckInterpreter;
import org.mitre.scap.xccdf.check.complexcheck.ComplexCheckResult;
import org.mitre.scap.xccdf.scoring.AbsoluteScoringModel;
import org.mitre.scap.xccdf.scoring.DefaultScoringModel;
import org.mitre.scap.xccdf.scoring.FlatScoringModel;
import org.mitre.scap.xccdf.scoring.FlatUnweightedScoringModel;
import org.mitre.scap.xccdf.scoring.Score;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlbeans.XmlString;


public class XCCDFProcessor {
    Map<RuleType, List<RuleResultType>> ruleToResultList = new LinkedHashMap<RuleType, List<RuleResultType>>();
    Map<RuleType, List<CheckResult>> ruleToCheckResultList = new LinkedHashMap<RuleType, List<CheckResult>>();

    private void appendTargetAndFacts(TestResultType results) {
        Set<String> targets = new LinkedHashSet<String>();
        Set<String> targetAddresses = new LinkedHashSet<String>();
        List<FactType> facts = new ArrayList<FactType>();


        // Lookup the necessary content
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e);
            else System.err.println("!! problem detecting network interfaces: " + e.getLocalizedMessage() );
        }

        if (networkInterfaces != null) {
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                if (interpreter.isVerboseOutput()) {
                    System.out.println("** processing interface: "+networkInterface.getDisplayName());
                }

                // InetAddresses
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // get hostname
                    String hostname = inetAddress.getHostName();
                    String fqdn = inetAddress.getCanonicalHostName();
                    String ip = inetAddress.getHostAddress();

                    String ipFactName = null;
                    if (inetAddress instanceof Inet4Address) {
//                        Inet4Address inet4Address = (Inet4Address)inetAddress;
                        ipFactName = "urn:xccdf:fact:asset:identifier:ipv4";
                    } else if (inetAddress instanceof Inet6Address) {
//                        Inet6Address inet6Address = (Inet6Address)inetAddress;
                        ipFactName = "urn:xccdf:fact:asset:identifier:ipv6";
                    }

                    targets.add(fqdn);
                    targetAddresses.add(ip);

                    facts.add(createFact(
                        "urn:xccdf:fact:asset:identifier:host_name",
                        ValueTypeType.STRING,
                        hostname
                    ));

                    facts.add(createFact(
                        "urn:xccdf:fact:asset:identifier:fqdn",
                        ValueTypeType.STRING,
                        fqdn
                    ));

                    facts.add(createFact(
                        ipFactName,
                        ValueTypeType.STRING,
                        ip
                    ));
                    if (interpreter.isVerboseOutput())
                        System.out.println("  InetAddr: hostname="+hostname+", fqdn="+fqdn+", "+ipFactName+"="+ip);
                }

                try {
                    Method method = NetworkInterface.class.getDeclaredMethod("getHardwareAddress");

                    byte[] bytes = (byte[])method.invoke(networkInterface);
                    if (bytes != null) {
                        String mac = ByteUtil.toHexString(bytes, ':');

                        if (interpreter.isVerboseOutput())
                            System.out.println("  MAC: "+mac);

                        facts.add(createFact(
                            "urn:xccdf:fact:asset:identifier:mac",
                            ValueTypeType.STRING,
                            mac
                        ));
                    }
                } catch (IllegalAccessException e) {
                    if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e);
                    else System.err.println("!! " + e.getLocalizedMessage() );
                } catch (IllegalArgumentException e) {
                    if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e) ;
                    else System.err.println("!! " + e.getLocalizedMessage() );
                } catch (InvocationTargetException e) {
                    if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e);
                    else  System.err.println("!! " + e.getLocalizedMessage() );
                } catch (NoSuchMethodException e) {
                    // Do not include the fact
                }
            }
            if (interpreter.isVerboseOutput()) {
                System.out.println();
            }
        }

        // Append the results to the XML
        if (targets.isEmpty()) {
            targets.add("unknown");
        }

        // Build targets
        for (String target : targets) {
            XmlString xmlString = results.addNewTarget();
            xmlString.setStringValue(target);
        }

        // Build target-addresses
        for (String targetAddress : targetAddresses) {
            XmlString xmlString = results.addNewTargetAddress();
            xmlString.setStringValue(targetAddress);
        }

        if (!facts.isEmpty()) {
            TargetFactsType factsType = results.addNewTargetFacts();
            for (FactType fact : facts) {
                factsType.addNewFact().set(fact);
            }
        }
    }

    public enum RuleAction {
        NOTCHECKED,
        CHECK,
        NOTSELECTED,
        NO_SUPPORTED_CHECK,
        ERROR,
        NOTAPPLICABLE;
    }

    private static Logger log = Logger.getLogger(XCCDFProcessor.class.getName());

    private final Map<RuleType,RuleAction> ruleActionMap;
    private final Map<RuleType,List<MessageType>> ruleMessageMap = new HashMap<RuleType,List<MessageType>>();
    private final XCCDFInterpreter interpreter;
    private final CPEResolver cpeResolver;


    public XCCDFProcessor(
            final XCCDFInterpreter interpreter,
            final CPEResolver cpeResolver) {
        Collection<RuleType> rules = interpreter.getRules();

        ruleActionMap = new LinkedHashMap<RuleType,RuleAction>(rules.size());
        this.interpreter = interpreter;
        this.cpeResolver = cpeResolver;
    }

    public static String getResultId(
            final String benchmarkId,
            final String profileId,
            final Calendar startTime) {
        // Build a result id
        StringBuilder resultId = new StringBuilder(benchmarkId);
        if (profileId != null) {
            resultId.append('-');
            resultId.append(profileId);
        }
        resultId.append('-');
        resultId.append(startTime.getTimeInMillis());
        return resultId.toString();
    }

    public static TextType getTitle(
            final String benchmarkId,
            final String profileId,
            final Calendar startTime) {
        TextType retval = TextType.Factory.newInstance();
        retval.setLang("en-US");

        StringBuilder titleText = new StringBuilder("SCAP automated assessment for checklist ");
        titleText.append(benchmarkId);
        if (profileId != null) {
            titleText.append(" using profile ");
            titleText.append(profileId);
        }
        titleText.append(" performed at ");
        titleText.append(DateFormat.getDateInstance(DateFormat.FULL).format(startTime.getTime()));
        retval.setStringValue(titleText.toString());
        return retval;
    }

    public BenchmarkDocument process() {
        /*
         * Benchmark.Content - For each Item in the Benchmark object’s items
         * property, initiate Item.Process
         */
        Calendar startTime = Calendar.getInstance();

        BenchmarkDocument retval = interpreter.getDocument();
        BenchmarkDocument.Benchmark benchmark = retval.getBenchmark();
        TestResultType results = benchmark.addNewTestResult();

        results.setId(getResultId(
                benchmark.getId(),
                interpreter.getProfileId(),
                startTime));

        results.setVersion(benchmark.getVersion().getStringValue());

        TextType title = results.addNewTitle();
        title.set(getTitle(
                benchmark.getId(),
                interpreter.getProfileId(),
                startTime));

        if (interpreter.getProfileId() != null) {
            IdrefType profile = IdrefType.Factory.newInstance();
            profile.setIdref(interpreter.getProfileId());
            results.setProfile(profile);
        }

        results.setTestSystem(BuildProperties.getInstance().getApplicationName()+" "+BuildProperties.getInstance().getApplicationVersion());

        appendTargetAndFacts(results);

        CheckInterpreter        checkInterpreter        = null;
        ComplexCheckInterpreter complexCheckInterpreter = null;

        if (interpreter.isProcessChecks()) {
            checkInterpreter        = new CheckInterpreter(interpreter);
            complexCheckInterpreter = new ComplexCheckInterpreter( interpreter );
        }

        XCCDFVisitor.visit(interpreter.getDocument(),new ProcessorXCCDFVisitorHandler( checkInterpreter, complexCheckInterpreter ));

        if (checkInterpreter != null) {
            checkInterpreter.processChecks();
            for (Map.Entry<String,String> entry : checkInterpreter.getValuesUsedMap().entrySet()) {
                ProfileSetValueType setValue = results.addNewSetValue();
                setValue.setIdref(entry.getKey());
                setValue.setStringValue(entry.getValue());
            }
        }

        if( complexCheckInterpreter != null ){
            complexCheckInterpreter.processComplexChecks();
            for (Map.Entry<String,String> entry : complexCheckInterpreter.getValuesUsedMap().entrySet()) {
                ProfileSetValueType setValue = results.addNewSetValue();
                setValue.setIdref(entry.getKey());
                setValue.setStringValue(entry.getValue());
            }
        }

        for (Map.Entry<RuleType, RuleAction> entry : ruleActionMap.entrySet()) {
            RuleType rule = entry.getKey();
            RuleAction action = entry.getValue();
            
            switch (action) {
                case NOTCHECKED: {
                    RuleResultType ruleResult = results.addNewRuleResult();
                    buildRuleResultBase(rule,ruleResult);
                    ruleResult.setResult(ResultEnumType.NOTCHECKED);
                    break;
                }
                case CHECK:
                    if (checkInterpreter != null && complexCheckInterpreter != null ) {
                        processCheckedRule(checkInterpreter, complexCheckInterpreter, rule, results);
                    } else {
                        RuleResultType ruleResult = results.addNewRuleResult();
                        buildRuleResultBase(rule,ruleResult);
                        ruleResult.setResult(ResultEnumType.NOTCHECKED);
                    }
                    break;
                case NOTSELECTED: {
                    RuleResultType ruleResult = results.addNewRuleResult();
                    buildRuleResultBase(rule,ruleResult);
                    ruleResult.setResult(ResultEnumType.NOTSELECTED);
                    break;
                }
                case NO_SUPPORTED_CHECK: {
                    RuleResultType ruleResult = results.addNewRuleResult();
                    buildRuleResultBase(rule,ruleResult);
                    ruleResult.setResult(ResultEnumType.NOTCHECKED);
                    break;
                }
                case ERROR: {
                    RuleResultType ruleResult = results.addNewRuleResult();
                    buildRuleResultBase(rule,ruleResult);
                    ruleResult.setResult(ResultEnumType.ERROR);
                    break;
                }
                case NOTAPPLICABLE: {
                    RuleResultType ruleResult = results.addNewRuleResult();
                    buildRuleResultBase(rule,ruleResult);
                    ruleResult.setResult(ResultEnumType.NOTAPPLICABLE);
                }
            }

        }

        /*
         * Benchmark.Back - Perform any additional processing of the Benchmark
         * object properties
         */
        if (interpreter.isVerboseOutput()){
            System.out.println(" - Benchmark.Back");
        }

        // Score calculations
        Map<String,ScoringModel> scoringModels = new HashMap<String,ScoringModel>();
        ScoringModel model;

        model = new DefaultScoringModel();
        scoringModels.put(model.getSystem(),model);
        model = new FlatScoringModel();
        scoringModels.put(model.getSystem(),model);
        model = new FlatUnweightedScoringModel();
        scoringModels.put(model.getSystem(),model);
        model = new AbsoluteScoringModel();
        scoringModels.put(model.getSystem(),model);

        Map<String,ModelDocument.Model> requiredModels = interpreter.getSupportedScoringModels();
        for (Map.Entry<String,ScoringModel> entry : scoringModels.entrySet()) {
            String system = entry.getKey();
            if (requiredModels.containsKey(system)) {
                Score score = model.score(interpreter, results,requiredModels.get(system));
                ScoreType scoreType = results.addNewScore();
                scoreType.setSystem(model.getSystem());
                scoreType.setMaximum(score.getMaximumScore());
                scoreType.setBigDecimalValue(score.getScore());
            }
        }

        results.setEndTime(Calendar.getInstance());

        return retval;
    }


    /*
    private void printRuleResult( RuleType rule, List<CheckResult> results, int lineLength ){
      int ruleIdLen = rule.getId().length();

      for( CheckResult result : results ){
        for( CheckContentRefType content : result.getCheckResult().getCheckContentRefList() ){
          String output = String.format("   %1$s $" + (lineLength - ruleIdLen ) + "s%3$s", rule.getId(), "", result.toUpperCase() );
          System.out.println( output );
        }
      }
    }
   */


    private void printRuleResult( RuleType rule, String result, int lineLength ){
      int ruleIdLen = rule.getId().length();
      String output = String.format("   %1$s %2$" + (lineLength - ruleIdLen ) + "s%3$s", rule.getId(), "", result.toUpperCase() );
      System.out.println( output );
    }



    public void printRuleResults(){
      System.out.println(" - RULE RESULTS:");

      int   longestRuleId = 0;

      for( RuleType rule : this.ruleActionMap.keySet() ){
        int idLength = rule.getId().length();
        if( idLength > longestRuleId )  longestRuleId = idLength;
      }

      int lineLength = longestRuleId + 10;

      for( Map.Entry<RuleType, RuleAction> entry : this.ruleActionMap.entrySet() ){
        RuleType    rule          = entry.getKey();
        RuleAction  action        = entry.getValue();

        if( action.equals( RuleAction.CHECK ) == false ){
          this.printRuleResult( rule, action.toString(), lineLength );
        }
        else{
          List<RuleResultType> results = this.ruleToResultList.get( rule );
          for( RuleResultType result : results ){
            this.printRuleResult( rule, result.getResult().toString(), lineLength );
          }
        }

      }
    }




    private void determineRuleRoleResult( RuleType rule, RuleResultType ruleResultType, ResultEnumType.Enum resultEnum ){
        /*
         * full = if the rule is selected, then check it and let the result
         *      contribute to the score and appear in reports (default, for
         *      compatibility for XCCDF 1.0).
         * unscored = check the rule, and include the results in any report, but
         *      do not include the result in score computations (in the default
         *      scoring model the same effect can be achieved with weight=0)
         * unchecked = don't check the rule, just force the result status to
         *      'unknown'.  Include the rule's information in any reports.
         */
        if (rule.isSetRole()) {
            switch (rule.getRole().intValue()) {
                case RoleEnumType.INT_UNSCORED:
                    ruleResultType.setResult(ResultEnumType.INFORMATIONAL);
                    break;
                case RoleEnumType.INT_UNCHECKED:
                    ruleResultType.setResult(ResultEnumType.NOTCHECKED);
                    break;
                case RoleEnumType.INT_FULL:
                default:
                    ruleResultType.setResult( resultEnum );
            }
        } else {
            ruleResultType.setResult( resultEnum );
        }
    }



    private void processCheckResultMessages( RuleResultType ruleResultType, List<MessageType> messages ){
        if (messages.size() > 0) {
            ruleResultType.setMessageArray(messages.toArray(new MessageType[messages.size()]));
        }
    }


    private void applyCheckToRuleResult( RuleType rule, RuleResultType ruleResultType, List<CheckType> checks ){
      if (!rule.isSetRole() || rule.getRole() != RoleEnumType.UNCHECKED) {
        for( CheckType checkType : checks ){
          if (checkType != null) {
            CheckType data = ruleResultType.addNewCheck();
            data.set(checkType);
          }
        }
      }
    }



    private void applyInstancesToRuleResult( RuleResultType ruleResult, CheckResult checkResult ){
      if (checkResult.isInstance()) {
        InstanceResultType instance = ruleResult.addNewInstance();
        instance.set(checkResult.getInstance());
      }
    }



    private void processCheckedRule(
            final CheckInterpreter checkInterpreter,
            final ComplexCheckInterpreter complexCheckInterpreter,
            final RuleType rule,
            final TestResultType results) {

        boolean hasComplexCheck = rule.isSetComplexCheck();
        
        List<RuleResultType>  resultList      = this.ruleToResultList.get( rule );
        //List<CheckResult>     checkResultList = this.ruleToCheckResultList.get( rule );

        if( resultList == null ){
          resultList = new ArrayList<RuleResultType>();
          this.ruleToResultList.put( rule, resultList );
        }

        if ( hasComplexCheck == true ){
          ComplexCheckResult complexResult  = complexCheckInterpreter.getComplexCheckResult( rule );
          List<CheckResult> checkResults    = complexResult.getChecks();

          this.ruleToCheckResultList.put( rule, checkResults );

          RuleResultType ruleResult = results.addNewRuleResult();
          buildRuleResultBase( rule, ruleResult );
          
          this.determineRuleRoleResult(rule, ruleResult, complexResult.getResult());
          for( CheckResult checkResult : checkResults ){
            this.processCheckResultMessages(ruleResult, checkResult.getMessages());
            this.applyInstancesToRuleResult( ruleResult, checkResult );
            this.applyCheckToRuleResult( rule, ruleResult, Collections.singletonList( checkResult.getCheckResult() ) );
          }

          resultList.add( ruleResult );
        }
        else {
          List<CheckResult> checkResults  = checkInterpreter.getCheckResults( rule );
          this.ruleToCheckResultList.put( rule, checkResults );

          for (CheckResult checkResult : checkResults ) {
            RuleResultType ruleResult = results.addNewRuleResult();
            buildRuleResultBase(rule, ruleResult);
            
            this.determineRuleRoleResult(rule, ruleResult, checkResult.getResult());
            this.processCheckResultMessages(ruleResult, checkResult.getMessages());
            this.applyInstancesToRuleResult( ruleResult, checkResult );
            this.applyCheckToRuleResult( rule, ruleResult, Collections.singletonList( checkResult.getCheckResult() ) );

            resultList.add( ruleResult );
          }
        }
    }

    private void buildRuleResultBase(
            final RuleType rule,
            final RuleResultType ruleResult) {

        // Attributes
        ruleResult.setIdref(rule.getId());
        if (rule.isSetRole()) ruleResult.setRole(rule.getRole());
        if (rule.isSetSeverity()) ruleResult.setSeverity(rule.getSeverity());
        ruleResult.setTime(Calendar.getInstance());
        if (rule.isSetVersion()) ruleResult.setVersion(rule.getVersion().getStringValue());
        if (rule.isSetWeight()) ruleResult.setWeight(rule.getWeight());

        // Elements
        if (rule.sizeOfIdentArray() > 0) {
            ruleResult.setIdentArray(rule.getIdentList().toArray(new IdentType[rule.sizeOfIdentArray()]));
        }

        // TODO: P5: support fix

        if (ruleMessageMap.containsKey(rule)) {
            for (MessageType messageType : ruleMessageMap.get(rule)) {
                MessageType message = ruleResult.addNewMessage();
                message.set(messageType);
            }
        }
    }

    private void appendMessageToRuleResult(
            final RuleType rule,
            final MsgSevEnumType.Enum severity,
            final String msg) {
        List<MessageType> list = ruleMessageMap.get(rule);
        if (list == null) {
            list = new ArrayList<MessageType>();
            ruleMessageMap.put(rule, list);
        }
        MessageType message = MessageType.Factory.newInstance();
        message.setSeverity(severity);
        message.setStringValue(msg);
        list.add(message);
    }

    public static FactType createFact(
            final String name,
            final ValueTypeType.Enum type,
            final String value) {
        FactType retval = FactType.Factory.newInstance();
        retval.setName(name);
        retval.setType(type);
        retval.setStringValue(value);
        return retval;
    }

    public class ProcessorXCCDFVisitorHandler extends XCCDFVisitorHandler {

        private final Map<SelectableItemType,Boolean> selectedMap = new HashMap<SelectableItemType,Boolean>();
        private final CheckInterpreter checkInterpreter;
        private final ComplexCheckInterpreter complexCheckInterpreter;

        public ProcessorXCCDFVisitorHandler( final CheckInterpreter checkInterpreter, final ComplexCheckInterpreter complexCheckInterpreter ) {
            this.checkInterpreter         = checkInterpreter;
            this.complexCheckInterpreter  = complexCheckInterpreter;
        }

        public XCCDFInterpreter getXCCDFInterpreter() {
            return interpreter;
        }

        private boolean isApplicablePlatform(
                final SelectableItemType item,
                final BenchmarkDocument.Benchmark benchmark,
                final GroupType parent) {

            boolean retval = false;
            String match = null;
            if (!interpreter.isProcessCPE()) {
                retval = true;
            } else if (item.sizeOfPlatformArray() > 0) {
                // process CPEs
                for (OverrideableURIidrefType cpeId : item.getPlatformList()) {
                    try {
                        if (cpeResolver.evaluateCPE(cpeId.getIdref()) == CPEResolver.Status.PASS) {
                            match = cpeId.getIdref();
                            retval = true;
                            break;
                        }
                    } catch (URISyntaxException ex) {
                      if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, ex);
                      else System.err.println("!! " + ex.getLocalizedMessage() );
                    }
                }
            } else {
                retval = true;
            }

            if (interpreter.isVerboseOutput()) {
                if (retval && match != null) {
                    System.out.println("platform match: "+match);
                } else if (!retval) {
                    System.out.println("platform match: failed");
                }
            }
            return retval;
        }

        @SuppressWarnings("unchecked")
		private boolean isSelected(
                final SelectableItemType item,
                final BenchmarkDocument.Benchmark benchmark,
                final GroupType parent) {

            boolean retval;
            if (selectedMap.containsKey(item)) {
                retval = selectedMap.get(item);
            } else {
                boolean parentSelected;
                if (parent != null) {
                    // We can assume that the parent has been visited already
                    parentSelected = selectedMap.get(parent);
                } else {
                    // Parent is benchmark, always true
                    parentSelected = true;
                }

                if (!parentSelected) {
                    retval = false;
                } else {
                    retval = item.getSelected();
                }

                if (retval) {
                    // Only process selected items, since the result is possible unselection
                    /*
                     * Item.Process - Check the contents of the requires and conflicts
                     * properties, and if any required Items are unselected or any
                     * conflicting Items are selected, then set the selected and
                     * allowChanges properties to false.
                     */
                    if (interpreter.isVerboseOutput()) {
                        System.out.println(" - Item.Process: "+item.getId());
                    }
                    // TODO: what is allowChanges? prohibitChanges?
                    requires: for (IdrefListType idList : item.getRequiresList()) {
                        boolean requiresMet = false;
                        for (String id : (List<String>)idList.getIdref()) {
                            SelectableItemType requiredItem = getXCCDFInterpreter().lookupSelectableItem(id);
                            if (requiredItem == null) {
                                if (interpreter.isVerboseOutput()) log.severe("required item '"+id+"' not found");
                            } else if ( requiredItem.getSelected() ) {
                              requiresMet = true;
                              break;
                            }
                        }

                        if( requiresMet == false ){
                            if (interpreter.isVerboseOutput()) {
                                  System.out.println("!! requires failed: disabling item: "+item.getId());
                            }

                            item.setSelected(false);
                            item.setProhibitChanges(true);
                            retval = false;
                            break requires;
                        }
                    }
                }

                if (retval) {
                    conflicts: for (IdrefType idList : item.getConflictsList()) {
                        String id  = idList.getIdref();
                        SelectableItemType conflictingItem = getXCCDFInterpreter().lookupSelectableItem(id);
                        if (conflictingItem == null) {
                            if (interpreter.isVerboseOutput()) log.severe("conflicting item '"+id+"' not found");
                        } else if ( conflictingItem.getSelected() ) {
                            if (interpreter.isVerboseOutput()){
                                System.out.println("WARNING: conflicting item '"+id+"' is selected. Disabling item: "+item.getId());
                            }
                            
                            item.setSelected(false);
                            item.setProhibitChanges(true);
                            retval = false;
                            break conflicts;
                        }
                    }
                }
                
                selectedMap.put(item, retval);
            }

            return retval;
        }

        @Override
        public void visitValue(
                final ValueType value,
                final BenchmarkDocument.Benchmark benchmark,
                final GroupType parent) {
            /*
             * Value.Content - If the Item is a Value, then process the properties
             * of the Value.
             */
            
            boolean selected = ( (parent == null) || selectedMap.get( parent ) );
            if (selected) {
                if (interpreter.isVerboseOutput()) {
                    System.out.println(" - Value.Content: "+value.getId());
                }
                processValueContent(value,benchmark,parent);
            } else {
                unprocessedValue(value,benchmark,parent,selected);
            }
        }

        @Override
        public boolean visitGroup(
                final GroupType group,
                final BenchmarkDocument.Benchmark benchmark,
                final GroupType parent) {
            boolean selected = isSelected(group,benchmark,parent);
            boolean applicablePlatform = isApplicablePlatform(group,benchmark,parent);

            /*
             * Item.Select - If any of the following conditions holds, cease
             * processing of this Item.
             *
             * 1. The processing type is Tailoring, and the optional property and
             *      selected property are both false.
             * 2. The processing type is Document Generation, and the hidden
             *      property is true.
             * 3. The processing type is Compliance Checking, and the selected
             *      property is false.
             * 4. The processing type is Compliance Checking, and the current
             *      platform (if known by the tool) is not a member of the set of
             *      platforms for this Item.
             */
            if (interpreter.isVerboseOutput())
                System.out.println(" - Item.Select: "+group.getId());

            if (selected && applicablePlatform) {
                processGroup(group,benchmark,parent);
            } else {
                unprocessedGroup(group,benchmark,parent,selected,applicablePlatform);
            }
            return true;
        }

        @Override
        public final void visitRule(final RuleType rule,final BenchmarkDocument.Benchmark benchmark,final GroupType parent) {
            boolean selected = isSelected(rule,benchmark,parent);
            boolean applicablePlatform = isApplicablePlatform(rule,benchmark,parent);

            /*
             * Item.Select - If any of the following conditions holds, cease
             * processing of this Item.
             *
             * 1. The processing type is Tailoring, and the optional property and
             *      selected property are both false.
             * 2. The processing type is Document Generation, and the hidden
             *      property is true.
             * 3. The processing type is Compliance Checking, and the selected
             *      property is false.
             * 4. The processing type is Compliance Checking, and the current
             *      platform (if known by the tool) is not a member of the set of
             *      platforms for this Item.
             */
            if (interpreter.isVerboseOutput())
                System.out.println(" - Item.Select: "+rule.getId());

            if (selected && applicablePlatform) {
                processRule(rule,benchmark,parent);
            } else {
                unprocessedRule(rule,benchmark,parent,selected,applicablePlatform);
            }
        }

        private final void processGroup(
                final GroupType group,
                final BenchmarkDocument.Benchmark benchmark,
                final GroupType parent) {
            /*
             * Group.Front - If the Item is a Group, then process the properties of
             * the Group.
             */
            if (interpreter.isVerboseOutput())
                System.out.println(" - Group.Front: "+group.getId());

            processGroupFront(group,benchmark,parent);

            /*
             * Group.Content - If the Item is a Group, then for each Item in the
             * Group’s items property, initiate Item.Process.
             */
            if (interpreter.isVerboseOutput())
                System.out.println(" - Group.Content: "+group.getId());

            processGroupContent(group,benchmark,parent);
        }

        private final void processRule(
                final RuleType rule,
                final BenchmarkDocument.Benchmark benchmark,
                final GroupType parent) {
            /*
             * Rule.Content - If the Item is a Rule, then process the properties of
             * the Rule.
             */
            if (interpreter.isVerboseOutput())
                System.out.println(" - Rule.Content: "+rule.getId());

            processRuleContent(rule,benchmark,parent);
        }

        protected void processValueContent(
                final ValueType value,
                final Benchmark benchmark,
                final GroupType parent) {
        }

        protected void unprocessedValue(
                final ValueType value,
                final Benchmark benchmark,
                final GroupType parent,
                final boolean selected) {
        }

        protected void processGroupFront(
                final GroupType group,
                final Benchmark benchmark,
                final GroupType parent) {
        }

        protected void processGroupContent(
                final GroupType group,
                final Benchmark benchmark,
                final GroupType parent) {
        }

        protected void unprocessedGroup(
                final GroupType group,
                final Benchmark benchmark,
                final GroupType parent,
                final boolean selected,
                final boolean applicablePlatform) {
        }



        protected void processCheck( CheckType check ){

        }



        protected void processRuleContent(
                final RuleType rule,
                final Benchmark benchmark,
                final GroupType parent) {

            if (checkInterpreter == null || complexCheckInterpreter == null
                    || (rule.isSetRole()
                    && rule.getRole() == RoleEnumType.UNCHECKED)) {
                ruleActionMap.put(rule, RuleAction.NOTCHECKED);
            } else {
                /*
                 * full = if the rule is selected, then check it and let the result
                 *      contribute to the score and appear in reports (default, for
                 *      compatibility for XCCDF 1.0).
                 * unscored = check the rule, and include the results in any report, but
                 *      do not include the result in score computations (in the default
                 *      scoring model the same effect can be achieved with weight=0)
                 * unchecked = don't check the rule, just force the result status to
                 *      'unknown'.  Include the rule's information in any reports.
                 */
                if (rule.isSetComplexCheck()) {
                  ComplexCheck      complexCheck      = null;
                  ComplexCheckType  complexCheckType  = rule.getComplexCheck();

                  try {
                    complexCheck  = new ComplexCheck( rule, complexCheckType, interpreter );
                    complexCheckInterpreter.registerComplexCheck( rule , complexCheck );
                    ruleActionMap.put( rule, RuleAction.CHECK );

                  } catch( ComplexCheckException ex ){
                    ruleActionMap.put(rule, RuleAction.ERROR);
                    appendMessageToRuleResult( rule,MsgSevEnumType.ERROR, ex.getErrorMessage() );
                  }

                } else {

                    RuleAction ruleAction = RuleAction.NOTCHECKED;
                    for (CheckType checkType : rule.getCheckList()) {
                        Check check;

                        String system = checkType.getSystem();
                        List<CheckExportType> checkExports = checkType.getCheckExportList();

                        CheckSystem checkSystem = CheckSystemRegistry.lookupCheckSystem(system);
                        if (checkSystem == null) {
                            System.err.println("!! unsupported check system " + system);
                            appendMessageToRuleResult(rule,MsgSevEnumType.WARNING,"unsupported check system "+system);
                            ruleAction = RuleAction.NO_SUPPORTED_CHECK;
                            continue;
                        }

                        if (checkType.isSetCheckContent()) {
                            System.err.println( "!! embedded check-content is not supported. Unable to process rule " + rule.getId());
                            appendMessageToRuleResult(rule,MsgSevEnumType.WARNING,"embedded check-content is not supported");
                            ruleAction = RuleAction.NO_SUPPORTED_CHECK;
                            continue;
                        }

                        List<CheckContentRefType> checkContent = checkType.getCheckContentRefList();
                        check = checkSystem.newCheck(checkContent,checkExports);

                        if (checkType.sizeOfCheckImportArray() > 0) {
                            System.err.println("!! use of check-import is not supported. Unable to completely process rule " + rule.getId());
                            appendMessageToRuleResult(rule,MsgSevEnumType.WARNING,"use of check-import is not supported.");
                        }

                        checkInterpreter.registerCheck(rule,check);
                        ruleAction = RuleAction.CHECK;
                        break;
                    }

                    ruleActionMap.put(rule, ruleAction);
                }
            }
        }

        protected void unprocessedRule(
                final RuleType rule,
                final Benchmark benchmark,
                final GroupType parent,
                final boolean selected,
                final boolean applicablePlatform) {
            if (!selected) {
                ruleActionMap.put(rule, RuleAction.NOTSELECTED);
            } else {
                ruleActionMap.put(rule, RuleAction.NOTAPPLICABLE);
            }
        }

    }
}
