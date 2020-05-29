# MDfromHTML
Generate Markdown from HTML using filters to remove noise from web pages (e.g., headers, footers, advertisements, sidebars). Captures provenance of markdown generation back to original HTML content and explains filtering that occurred. Also includes tools to generate formatted text from the generated markdown. This repo includes multiple Eclipse Maven Java projects including REST web services to generate MD from HTML.

## Project Components
  * html_extractor: HTML capture providing a web server for cURL requests or for interactive use to capture rendered web pages via Selenium and Chromium
  * MDfromHTMLBase: Common utility methods used by other projects
  * Remark: upgraded code from https://bitbucket.org/OverZealous/remark/src/default/ to provide HTML parsing and converstion to Markdown
  * MarkdownGenerator: Utilities and Services to perform Markdown generation from HTML
  * MDfromHTMLWebServices: WAR file generation of REST web services to  generate markdown form  HTML

## Building Projects
Each project can be  built by using the  command line: **mvn clean install** command in the project directory to write jar or war files to the target subdirectory. Alternativiely, right clicking the pom.xml file in Eclipse, selecting Run As... Maven build... and specifying  **clean  install** as the goals will build the project in Eclipse. 

### JDK Version
Content has been build using the Open  JDK version 1.8.0_242_b08 available  for download from https://adoptopenjdk.net/

### Eclipse Version
Projects  were developed in Eclipse 2020-03 available from  https://www.eclipse.org/downloads/ installing the Java EE Profile during installation.

## License
The  code  in this repository is licensed under the  Apache 2.0 License

## Support
It is best to open an issue in this repository. You may also contact Nathaniel Mills at wnm3@us.ibm.com.