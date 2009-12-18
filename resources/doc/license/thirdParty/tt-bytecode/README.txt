TechTrader Bytecode Toolkit
============================

Requirements
------------
- JDK 1.2 or greater


Overview
--------
The TechTrader Bytecode Toolkit provides high-level APIs for manipulating
Java bytecode.  It is completely open source under the BSD license.  To get
started with the toolkit, view the package Description for the
com.techtrader.modules.tools.bytecode package in the included javadoc.


Scripts
-------
bin/printclass:	Pretty-prints the bytecode in a .class file; similar to the
				'javap -c' command, but more detailed.  Each argument to the 
				script should be either the full name of a class to print, or 
				a .class file to print.
