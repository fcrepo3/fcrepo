some help with xacml


OASIS
http://docs.oasis-open.org/xacml/access_control-xacml-2_0-core-spec-cd-04.pdf 
this is a spec and a good reference for identifiers.  

Also, a nice intro to the xacml concepts is at:
http://www.oasis-open.org/committees/download.php/2713/Brief_Introduction_to_XACML.html

Other documents general to XACML are available at the homepage:
http://www.oasis-open.org/committees/tc_home.php?wg_abbrev=xacml

In writing policies, the following reference is useful:
http://www.zvon.org/xxl/XACML1.0/Output/index.html


SUN
Specific to the Sun Java reference implementation is the programmer's guide at
http://sunxacml.sourceforge.net/guide.html 
and the Javadocs at 
http://sunxacml.sourceforge.net/javadoc/index.html  
The project's homepage is at http://sunxacml.sourceforge.net/

XACML QUICKSTART / GOTCHAS
xacml provides attribute values for <Target> evaluation as single values, 
but provides them for <Condition> evaluation as "bags" (sets), doing so even 
for either singleton or empty bags.  Code policies accordingly.

In Targets, multiple <Subject> elements are logically or-ed.  Only one Subject matching
a request is necessary for that policy to apply.
  
Multiple <SubjectMatch> elements are logically and-ed.  All SubjectMatch elements must
match a request for that Subject to match.

The three higher-level divisions Subjects, Actions, and Resources must -all- match a
request, for the policy to be applicable.  At least one of its rule must also match, or 
the policy is deemed not applicable.  [A fourth division at this level, Environments, 
is apparent in the OASIS xacml spec, but not yet implemented by sunxacml.]

MatchId functions (which are used in Targets) are much restricted in allowed values, 
compared to the values allowed in the analogous FunctionIds (which are used in Conditions). 

There are no existing functions which are self-contained boolean combinations, like not-equal.
Since attributes are generally not boolean themselves (and so possibly negated), the not 
function can't be used as a MatchId, e.g., in a SubjectMatch element.  Since SubjectMatch, e.g.,
expresses a single binary operation, there is no possibility of introducing negative logic
into a Target.  [An exception would be an explicit value returned by an attribute finder, which 
would signify the absence of the attribute.]

<Subjects>, <Actions>, <Resources>, and <Environments> in <Target>s behave
similarly, in regard to all this.  Except . . .
despite statements that <Environments> was added to <Target> generally, 
it doesn't seem to work currently in sunxacml.

Targets can be directly under Policy or PolicySet, or directly under Rule.  Targets directly
under Policy apply to any contained Rule which doesn't have its own Target.  A Target directly
under a Rule overrides, for that Rule only, a Target directly under the containing Policy.

Conditions can appear only within a Rule, not directly under Policy or PolicySet.

All this combines to make Targets a appealing, frame-like expression, but with a constrained logic
which isn't always expressive enough, hence the need for Condition.  And, since Condition cannot 
apply to the Policy in whole, if Target cannot express a Policy-wide constraint, 
the Condition expresses this must be repeated under each Rule.

Another issue is that, with no short-circuiting in expressions and with no explicit way to test 
if an attribute is available, absence of an attribute causing an Indeterminate (error) result
is a practical impediment to writing effective policies.  [Hence, Fedora's attribute finders 
(callbacks for the sunxacml engine to use) return a dummy value if the attribute is not available.]  

sunxacml has a relaxed parsing of policies; e.g., we have encountered schema violation 
(e.g., Action omitted between Actions and ActionMatch) which resulted only in the policy not being
evaluated correctly, as opposed to failing parse.  How widespread this is, we don't know.
As a precaution, policies should be tested for effect.  This is good practice, anyway, since
testing is the only check of the policy-writer's understanding of xacml and against the 
inevitable typ0.

Though sunxacml parsing is relaxed, <Description> </Description> apparently requires at least one-character 
content:  <Desciption/> doesn't do it.

For now, there is no Fedora-based xacml schema checking of policies.  Some policies fail sunxacml parse
with console errors; with repository-policies, this would be at server startup, with object-policies, 
only on access to that specific object.  

on schema validation:
http://sunxacml.sourceforge.net/guide.html#using-validating


FEDORA

Whether Fedora uses xacml for authorization decisions is controlled broadly by 
authorization module parameter "ENFORCE-MODE", coded in the fedora.fcfg file.  Use one
of three values, with the following meanings: 
	"enforce-policies" == use xacml to determine whether a request is permitted or denied
	"permit-all-requests" == don't use xacml; permit every request
	"deny-all-requests" == don't use xacml; deny every request
The first of these is the usual setting.  The second can facilitate testing code independent 
of security.  The third could be used to quickly shut down access to the server, but requires
a server restart to effect this.

Tomcat container security is, of course, still a first barrier to authentication/authorization;
i.e., Fedora's web.xml specifies access protection earlier than xacml.  Tomcat container security 
is always in place regardless of the setting for parameter ENFORCE-MODE.

The Fedora-specific identifiers to use in policies can be found in 
dist/server/config/xacml-policies/vocabulary.txt

To activate policies, 
copy selected policies appropriate to your site, 
from subdirectories of dist/server/config/xml-policies/examples 
into subdirectories of dist/server/config/xml-policies/active; specifically:
 
from examples/repository-policies-approximating-2.0 into active/repository-policies;
or from examples/other-repository-policies into active/repository-policies;
or from examples/object-policies into active/object-policies, renaming files if appropriate;
The example repository policies in examples/repository-policies-approximating-2.0 approximate
the protection hardcoded into Fedora 2.0

An object policy named demo-5.xml in that directory will be included in 
evaluating authz for Fedora object demo:5  Or put the object policy in 
the object's datastream named "POLICY".  It is good practice with object
policy's to include a check of the pid in the policy:  if the policy mistakenly
gets put into repository-policies, it has the same effect.  

And the n repository policies are always in play.  So the number of policies 
set up by Fedora for the sunacml pdp to consider for a request is:

n	request doesn't refer to an object
	request refers to an object
n		no object policy
n+1		object policy in object
n+1		object policy in file
n+2		object policies in both object and file

These policies are combined programmatically dynamically per request into a PolicySet, 
whose combining algorithm is configured in fedora.fcfg  sunxacml evaluates a request against
this PolicySet.  

to do:  should one object policy override the other?  if so, what order of preference?

to do:  should there be a way for an object policy to override repository policies?  e.g., 
we could have 2 named policy datastreams COOPERATING-POLICY (like now) and OVERRIDING-POLICY
(which would cause the software to ignore a COOPERATING-POLICY datastream -and- all of
the repository policies.  This would be easy to do in the software.  This is solely a 
question of how we want this to work.

Duplicate and edit as needed to create your own policy mix.
Changes to repository-policies requires a server restart.
Use MSIE or an XML editor to check well-formedness after editing.
XML which violates the xacml schema might not show up until Fedora attempts to 
load the policy.

The example policies are crafted to be used together, and are of two broad types:
1. "positive" policies can only permit authz; these are named beginning "permit-"
2. "negative" policies can only deny authz; these are named beginning "deny-"

We are now discussing an alternate policy-writing approach, which focuses each Policy functionally, 
but which has it returning Permit or Deny according to several rules.  This is, in fact, the 
general case. 

Assuming that the configured combining algorithm is com.sun.xacml.combine.OrderedDenyOverridesPolicyAlg
and that the single-effect approach to writing policies is used, for a request to succeed authz, 
	1. at least one positive policy must match the request
	2. no negative policy can match the request
	3. no policy evaluations can return indeterminate 
		(an attribute was missing or an error prevented the evaluation)
	4. no policy evaluations can return an unanticipated result 
	5. some policy evaluations can return a notApplicable result
		(all needed attributes were present, but the policy's target wasn't matched)
	
	Because 3. severely cramps policy writing, Fedora xacml code always supplies an 
	attribute value (e.g., "" if otherwise absent).

Otherwise, the request fails authz.

to do:  document how to deal with "valueless" attributes as roles, e.g., administrator
http://lists.oasis-open.org/archives/xacml/200211/msg00193.html
MustBePresent

sunxacml returns a single result from its evaluation of the created PolicySet,
at least with the currently configured combining algorithm, but does so by returning a -set- 
of results.  The only evidence observed is that only one result is ever returned, and 
it is always one of the following:  Permit, Deny, NotApplicable, or Indeterminate.

Indeterminate is returned if an attribute value needed to evaluate a rule can't be found,
or another error prevented processing.

NotApplicable is returned if no rule applied and so couldn't return its effect.

Permit or Deny is returned if a rule did apply and thus returned its effect.

Or the combining algorithm of an individual policy or of the PolicySet might have converted, 
e.g., an Indeterminate into a Deny.

To protect against a possible bug in sunxacml code, Fedora's policy evaluation point (PEP) 
software additionally enforces the 5 items above.  If all are satisfied, operation proceeds
normally; if any are not, an Authorization fault is thrown.

Fedora's PEP builds a minimal request, which includes an index to an enhanced Fedora Context object.
There are 2 attribute finder modules provided as callbacks to the sunxacml engine.  The 
ResourceAttributeFinderModule provides only resource attribute values, only those it is coded explicitly 
to provide, and only those derived from actual objects in the repository.  The ContextAttributeFinderModule
provides any of the attribute types (subject, action, resource, or environment), as these have been
stored in an enhanced Fedora Context object.  It will honor a callback, even for an attribute which 
hasn't been explicitly coded, so can provide arbitrary attributes, e.g., from ldap lookup in a JAAS
login module.  [There are a few attributes which it explicitly doesn't serve, to prevent stack overflow
on improper recursion, or because the attributes are known to be provided in the xacml request itself.]

best practices:
1. policy-id and filename (- .xml) should match, for repository policies
2. policy-id and filename (- .xml) should match, for object policies in files, with concession to 
	demand of OS filenames
3. policy-id and PID should match, for object policies 
4. policy-id could include policy version, or this could be coded in description
5. policies should use simplest rule-combining algorithm which gives desired outcome; 
	avoid a more complicated algorithm which happens to work, but which confuses because 
	it implies more than what's there
6. an object policy should be coded so that it applies only to that specific object
7. it's better to fulfill expectations than to simplify a policy and break an expectation
(if most policies are single-effect, try to have all of them be single-effect, paid for by small policy
complication)
8. place AttributeValue before xAttributeDesignator as in the following snippet (xacml silently does this reordering,
leading to unexpected results, due to bad coding near the end of its TargetMatch.getInstance() method; I'll report
this bug, but for now, following this best practice will result in expected results, if perhaps harder to 
understand conditions) 
<ResourceMatch MatchId="urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than">
  <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#dateTime">2004-12-07T20:22:26.705Z</AttributeValue>
  <ResourceAttributeDesignator AttributeId="urn:fedora:names:fedora:2.1:resource:object:lastModifiedDate" 
    DataType="http://www.w3.org/2001/XMLSchema#dateTime"/>
</ResourceMatch>
9. avoid using two xAttributeDesignators (without any AttributeValues), until we clarify with Sun how they'll fix
this.


2005/04/20:  added second repository policy directory for programmatically generated policies
A policy in this directory is treated by Fedora authz code just like those in the original repository policy directory.


 
