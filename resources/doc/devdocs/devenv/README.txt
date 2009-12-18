To set up Eclipse (3.3 or Ganymede):

1) Go to "Window" -> "Preferences" -> "Java" -> "Code Style"
2) Under the top "Code Style" section:
   - enter "m_" for the prefix list for Fields, 
     and leave others blank
   - Check "Add @Override ...", and leave other checkboxes
     unchecked
   - Make sure "Exception variable name" is e
3) Import fedora-cleanup.xml under "Clean Up"
4) Replace "Firstname Lastname" with your own name in fedora-codetemplates.xml
4) Import fedora-codetemplates.xml under "Code Templates"
5) Import fedora-formatter.xml under "Formatter"
6) Import fedora.importorder under "Organize Imports"
7) Go to "Java" -> "Editor" -> "Save Actions"
8) Select "Perform the selected actions on save"
9) Make sure "Additional Actions" is selected
10) Click "Configure...", and go to the "Code Organizing" tab, and make sure "Remove trailing whitespace (All lines) is selected.

If using the Subversive plugin (recommended):
- Go to Team -> SVN -> Automatic Properties
- Import subversion.config

Also make sure Java 1.5 appears as an installed JRE and that it is the
default JRE for the project.