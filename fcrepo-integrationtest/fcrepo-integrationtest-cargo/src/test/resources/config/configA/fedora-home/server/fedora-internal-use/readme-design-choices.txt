this is an excerpt from an email written to sun.com explaining our use of xacml and sunxacml

1. only a few attributes are placed into the xacml request including an identifier for the context, 
the attributes of which are available on callback to an attribute finder module

2. this is mainly because we are providing a relatively rich set of attributes for use in policies, 
and I didn't want to have the usual request to have attributes which would mostly be unused

3. there is a second attribute finder module, to provide actual values from referenced resources 
so these can be used in policies, e.g., the state of an object
	
4. n -repository- policies are in play for any request
	
5. plus optional -per-object- policy, as xml file named for resource id (for testing) 

6. or as xml stored in the object itself

7. for now, both are in play, without any order of precedence
	
8. so there are from n to n+2 policies in play for a request
	
9. the interface code is not intended for exposure, but ours is an open source project, and so 
installations are free to do that
	
10. example policies are provided as equivalent to the more or less hardcoded authorization policy 
provided in Fedora < 2.1
	
11. policies are exposed as xml files or as components of repository objects
	
12. installations are expected to choose which provided policies are appropriate for their use, and 
to develop others to suit their needs, using our implemented vocabulary
	
13. I found it natural to write what seem to me to be mutually complementary policies, each of which 
has one rule and so a single effect.
	
14. Installations are free to follow or deviate from this style, and we expect multi-rule policies 
to be used, as well.

15. We are providing the equivalent of subject-id, action-id, and resource-id in our own identifiers.  
We came to that conclusion in reconciling the monolithic xacml resource category with our several kinds 
of resources -- e.g., object, datastream, etc.  After dealing with that, we sought uniformity by not 
using what appeared then to be stragglers:  subject-id and action-id.

16. So we're providing dummy values for subject-id, action-id, and resource-id, to fulfill syntax 
requirements on the xacml request.  

17. Another member of our team is working on a gui-based policy builder, which might be included in our 
2.1 release or could come along later.  The only thing I did along the way was to use a smallish class 
to read in a policy and to then use the stacktrace as some indication where my policy went wrong.

18.  We are considering how xacml policies could be used to encode and enforce workflow.