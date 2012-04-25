package org.fcrepo.server.security;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.xacml.PDPConfig;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeProxy;
 
import com.sun.xacml.attr.BaseAttributeFactory;
 
import com.sun.xacml.attr.StandardAttributeFactory;
 
import com.sun.xacml.combine.BaseCombiningAlgFactory;
 
import com.sun.xacml.combine.CombiningAlgFactory;
 
import com.sun.xacml.combine.CombiningAlgorithm;
 
import com.sun.xacml.combine.StandardCombiningAlgFactory;
 
import com.sun.xacml.cond.BaseFunctionFactory;
 
import com.sun.xacml.cond.BasicFunctionFactoryProxy;
 
import com.sun.xacml.cond.Function;
 
import com.sun.xacml.cond.FunctionFactory;
 
import com.sun.xacml.cond.FunctionFactoryProxy;
 
import com.sun.xacml.cond.FunctionProxy;
 
import com.sun.xacml.cond.StandardFunctionFactory;
 
import com.sun.xacml.cond.cluster.FunctionCluster;
 
import com.sun.xacml.finder.AttributeFinder;
 
import com.sun.xacml.finder.AttributeFinderModule;
 
import com.sun.xacml.finder.PolicyFinder;
 
import com.sun.xacml.finder.PolicyFinderModule;
 
import com.sun.xacml.finder.ResourceFinder;
 
import com.sun.xacml.finder.ResourceFinderModule;
 
public class PDPConfigurationFactory {
 
    public PDPConfigurationFactory() {
    }
 
    public AttributeFactory useStandardDatatypes(){
        return StandardAttributeFactory.getNewFactory();
    }
 
    public AttributeFactory useAttributeProxies(Map<String,AttributeProxy> proxies) {
        AttributeFactory result = new BaseAttributeFactory();
        for (String id:proxies.keySet()){
            result.addDatatype(id, proxies.get(id));
        }
        return result;
    }
 
    public CombiningAlgFactory useStandardAlgorithms(){
        return StandardCombiningAlgFactory.getNewFactory();
    }
 
    /**
     *
     * @param algorithms accepts a Set of CombiningAlgorithm impls
     * @return
     */
 
    public CombiningAlgFactory useAlgorithms(Set<CombiningAlgorithm> algorithms) {
        CombiningAlgFactory result = new BaseCombiningAlgFactory();
        for (CombiningAlgorithm algorithm: algorithms){
            result.addAlgorithm(algorithm);
        }
        return result;
    }
 
    public FunctionFactoryProxy useStandardFunctions(){
        return StandardFunctionFactory.getNewFactoryProxy();
    }
 
    public FunctionFactory useGeneralFunctions(Set<Function> functions,
            Map<String,FunctionProxy> proxies, List<FunctionCluster> clusters) throws URISyntaxException {
        return functionFactory(null,functions,proxies,clusters);
    }
 
    public FunctionFactory useConditionFunctions(FunctionFactory general, Set<Function> functions,
            Map<String,FunctionProxy> proxies, List<FunctionCluster> clusters) throws URISyntaxException {
        return functionFactory(general,functions,proxies,clusters);
    }
 
    public FunctionFactory useTargetFunctions(FunctionFactory conditions, Set<Function> functions,
            Map<String,FunctionProxy> proxies, List<FunctionCluster> clusters) throws URISyntaxException {
        return functionFactory(conditions,functions,proxies,clusters);
    }
 
    private FunctionFactory functionFactory(FunctionFactory base, Set<Function> functions,
            Map<String,FunctionProxy> proxies, List<FunctionCluster> clusters) throws URISyntaxException {
        FunctionFactory result = (base != null) ? new BaseFunctionFactory(base) : new BaseFunctionFactory();
        for (Function function:functions){
            result.addFunction(function);
        }
 
        for (String id:proxies.keySet()){
            result.addAbstractFunction(proxies.get(id), new URI(id));
        }
 
        for (FunctionCluster cluster:clusters){
            for (Object function:cluster.getSupportedFunctions()){
                result.addFunction((Function)function);
            }
        }
        return result;
    }

    public FunctionFactoryProxy useFunctionFactories(FunctionFactory target, FunctionFactory condition, FunctionFactory general){
        FunctionFactoryProxy result = new BasicFunctionFactoryProxy(target, condition, general);
        return result;
    }

    public PDPConfig getPDPConfig(List<AttributeFinderModule> attributeFinders,
                                  Set<PolicyFinderModule> policyFinders,
                                  List<ResourceFinderModule> resourceFinders) {
        AttributeFinder attr = new AttributeFinder();
        attr.setModules(attributeFinders);
        PolicyFinder policy = new PolicyFinder();
        policy.setModules(policyFinders);
        ResourceFinder rsrc = new ResourceFinder();
        rsrc.setModules(resourceFinders);
        return new PDPConfig(attr, policy, rsrc);
    }
    
    public PDPConfig getDefaultPDPConfig() {
        List<AttributeFinderModule> attributeFinders = new ArrayList<AttributeFinderModule>();
        Set<PolicyFinderModule> policyFinders = new HashSet<PolicyFinderModule>();
        List<ResourceFinderModule> resourceFinders = new ArrayList<ResourceFinderModule>();
        // defaults?
        return getPDPConfig(attributeFinders, policyFinders, resourceFinders);
    }

}