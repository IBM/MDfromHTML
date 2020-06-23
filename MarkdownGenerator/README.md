## MarkdownGenerator Project Information##

Note: this project depends upon the [Remark][] project. 
This project provides utilities to transform html captured in json files into markdown; and markdown into text.

The software uses a default directory for input and output: src/test/resources

### Building jar files ###

To build the jar files, do the following:

#### Ensure you have a settings.xml file that can access standard Maven repos ####

To configure Eclipse to use these settings
  1. Ensure there is a .m2 directory in your home directory:
     * cd ~ (or cd \Users\<your_id> on Windows)
     * ls -la .m2 (notice the dot prefix)
     * If there is no .m2 directory create one using mkdir .m2 (or md .m2 on Windows)
  2. Ensure there is a copy of the above in the settings.xml there
     * If there is no settings.xml use a text editor to create one and paste in the following, otherwise, merge in the server and repository entries from below. Replace "your_intranet_email_here" and "your_API_Key_here"

```
<?xml version="1.0" encoding="UTF-8"?>
<settings
   xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd"
   xmlns="http://maven.apache.org/SETTINGS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
   <servers>
   </servers>
   <profiles>
      <profile>
         <id>standard-extra-repos</id>
         <!--Override the repository (and pluginRepository) "central" from the
         Maven Super POM -->
         <repositories>
            <repository>
               <id>central</id>
               <url>https://repo1.maven.org/maven2</url>
               <releases>
                  <enabled>true</enabled>
               </releases>
            </repository>
         </repositories>
         <pluginRepositories>
            <pluginRepository>
               <id>central</id>
               <url>https://repo1.maven.org/maven2</url>
               <releases>
                  <enabled>true</enabled>
               </releases>
            </pluginRepository>
         </pluginRepositories>
      </profile>
   </profiles>
   <activeProfiles>
      <activeProfile>standard-extra-repos</activeProfile>
   </activeProfiles>
</settings>      
```

#### From command line ####

  1. Change to the project directory.
  2. Issue the command: mvn install clean 
     * This assumes you have installed mvn from [Maven Downloads][Maven_Downloads]

#### From Eclipse ####

  1. Ensure you have build the MDfromHTMLBase project's jars (e.g., right click on the pom.xml in the MDfromHTMLBase project, and select Run as / Maven build... and enter **clean install** as the Goals so the jar is stored in your ~/.m2/repository directory tree)
  2. Ensure you have build the Remark project's jars (e.g., right click on the pom.xml in the Remark project, and select Run as / Maven build... and enter **clean install** as the Goals so the jar is stored in your ~/.m2/repository directory tree)
  3. Right click on the pom.xml in the MarkdownGenerator project
  4. Select Run as / Maven build...
  5. Enter **clean install** as the Goals
  6. Click Launch. This will build two jar files in the target directory:
  
    * MarkdownGenerator-1.0.1-jar-with-dependencies.jar
    * MarkdownGenerator-1.0.1.jar

The MarkdownGenerator-1.0.1-jar-with-dependencies.jar contains all the dependent classes so we use this when running from a command line. Open the processAll.sh script in a Text Editor to see how this is accomplished.

## Execution Pipeline ##

### Generate Markdown ###

To generate markdown from html, you run the GetMarkdownFromHTML class. You can right click on the src/main/java/com.mdfromhtml.markdown.transform/GetMarkdownFromHTML.java and select Run as... / Java Application. 

If you'd prefer to run from the command line, go to the MarkdownGenerator project directory and issue a command like this:
```
java -cp "./target/MarkdownGenerator-1.0.1-jar-with-dependencies.jar" com.mdfromhtml.markdown.transform.GetMarkdownFromHTML "./src/test/resources" "./src/test/resources" true
```
or see the runMDGen.sh shell script

```
Enter the fully qualified path to directory containing json html capture files, or q to exit (./src/test/resources):

Enter the fully qualified path to the markdown output directory, or q to exit (./src/test/resources):

Files ending with .json will be read from ./src/test/resources
and the generated markdown (.md), and html (.html and _foramtted.html) saved in ./src/test/resources/
Press q to quit or press Enter to continue...
```

This will generate the following set of files for each input json file in the specified output directory:

  * X_formatted.html -- the original html from the input json file formatted in a hierarchical dom format.
  * X_html -- the unformatted original html from the input json file.
  * X.md -- the markdown file
  * X_html2md.json -- the provenance file tracking where the markdown came from in the formatted html file.

The program reads the **HTML_Filters.json** file to understand what html should be ignored when generating markdown.

### Generate Text ###

To generate text from markdown, you run the GetTextFromMarkdown class. You can right click on the src/main/java/com.mdfromhtml.markdown.transform/GetTextFromMarkdown.java and select Run as... / Java Application.

If you'd prefer to run from the command line, go to the MarkdownGenerator project directory and issue a command like this:
```
java -cp "./target/MarkdownGenerator-1.0.1-jar-with-dependencies.jar" com.mdfromhtml.markdown.transform.GetTextFromMarkdown "./src/test/resources" "./src/test/resources" true
```
or see the runTXTGen.sh shell script

```
Enter the fully qualified path to directory containing md multimarkdown files, or q to exit (./src/test/resources):

Enter the fully qualified path to the text file output directory, or q to exit (./src/test/resources):


Files ending with .md will be read from ./src/test/resources
and the generated text files (.txt) will be saved in ./src/test/resources
Press q to quit or press Enter to continue...
```

This will generate the following set of files for each input md file in the specified output directory:

  * X.txt -- the text file
  * X_md2txt.json -- the provenance file tracking where the text came from in the markdown file.

## Exploring Markdown Provenance ##

You can run the FindHTMLFromMarkdown class to find the HTML node corresponding to the markdown generated in the .md file. Right click on the src/main/java/com.mdfromhtml.provenance.FindHTMLFromMarkdown.java and select Run as... / Java Application.

```
Enter the fully qualified path to directory containing json html2md provenance files, or q to exit (./src/test/resources):

Enter the file name of the html2md provenance file, or q to exit (Archive0001_001_html2md.json):

Loading the input file ./src/test/resources/markdown/Archive0001_001_html2md.json
Press q to quit or press Enter to continue...

 0: # Install Skype for Business #
 1: Office for business
 2: Office 365 Admin
 3: Office 365 Small Business
 4: Office 365 Small Business Admin
 5: Skype for Business
 6: Skype for Business Online
 7: Office\.com
 8: Skype for Business Basic
 9: Skype for Business for Android
Enter line number, > (or n) for next page, < (or p) for prior page, s for search, q to quit:
```
If you have further questions, please contact Nathaniel Mills wnm3@us.ibm.com

[Remark]: https://github.com/IBM/MDfromHTML/tree/dev/Remark
[Maven_Downloads]: http://maven.apache.org/download.cgi
