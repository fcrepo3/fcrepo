To set up Eclipse (3.3 Ganymede - 3.5 Galileo):

 1) Go to "Window" -> "Preferences" -> "Java" -> "Code Style"
 2) Under the top "Code Style" section:
   - enter "m_" for the prefix list for Fields, and leave others blank
   - Check "Add @Override ...", and leave other checkboxes unchecked
   - Make sure "Exception variable name" is e
 3) Import fedora-cleanup.xml under "Clean Up"
 4) Replace "Firstname Lastname" with your own name in fedora-codetemplates.xml
 5) Import fedora-codetemplates.xml under "Code Templates"
 6) Import fedora-formatter.xml under "Formatter"
 7) Import fedora.importorder under "Organize Imports"
 8) Go to "Java" -> "Editor" -> "Save Actions"
 9) Select "Perform the selected actions on save"
10) Make sure "Additional Actions" is selected
11) Click "Configure...", and go to the "Code Organizing" tab, and make sure 
    "Remove trailing whitespace (All lines) is selected.
12) Go "Team" -> "Ignored Resources" and add ignore patterns:
	- .classpath
	- .project
	- .settings
	- *.prefs
	- target

If using the Subversive plugin (recommended):
- Go to Team -> SVN -> Automatic Properties
- Import subversion.config

Also make sure Java 1.6 appears as an installed JRE and that it is the
default JRE for the project.