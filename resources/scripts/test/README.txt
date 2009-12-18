                           Fedora Test Scripts 
                         =======================

These scripts are used by our continuous integration service:

    http://fedora-commons.org/bamboo

They can also be used to support running tests in your own development
environment.  Before using these scripts in your environment, you should
modify the following:

Configuration Files:
--------------------

    env.sh             Modify the variables in this script as appropriate for 
                       your environment.  See the comments in the script for
                       details.

    sanity.sh          Modify the -Dfedora.hostname and -Dfedora.port values as
                       appropriate.

    *.properties       These are install.properties files for automatically
                       installing specific configurations of the Fedora
                       server for testing.  As with env.sh, these files 
                       each need to be partially modified as appropriate
                       for your environment before running the scripts.

Scripts:
--------

    Note: Each of the following scripts returns 0 if successful, 1 otherwise.

    on-commit.sh       The entry point for our "on commit" builds.
                       As of this writing, this simply runs the sanity.sh
                       script with "java5" as a parameter.  This script
                       does not accept any arguments.

    sanity.sh          Runs a series of "sanity" tests using the given
                       version of java (java5 or java6) as a parameter.
                       As of this writing, this includes all unit tests
                       and the following integration tests:

                         configA
                         configB
                         configQ

                       For the integration tests, it automatically installs Fedora
                       with the proper configuration and starts Tomcat
                       before commencing with the test, and shuts down Tomcat
                       when the tests complete (whether successful or not).

                       Example Usage:
                         sanity.sh java5

    install-fedora.sh  Assuming a "release" build has been run, this installs
                       a fresh instance of Fedora with the given install.properties
                       file (the second argument).  The installer is executed
                       with the version of java given in the first argument
                       (java5 or java6).

                       Example Usage:
                         install-fedora.sh java5 ConfigB.properties
