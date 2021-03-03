#! /bin/bash
jar -uvf ./MarkdownGenerator-1.0.8-jar-with-dependencies.jar HTML_Filters.json
java -cp "./MarkdownGenerator-1.0.8-jar-with-dependencies.jar" com.mdfromhtml.markdown.transform.GetMarkdownFromHTML "./src/test/resources" "./src/test/resources"
ls -l ./src/test/resources
echo "Now load the .md file from the ./data/md directory in Chrome"
echo "Use a text editor to review the htmljson_####_html2md.json to see filters applied"
echo "Edit the HTML_Filters.json and try again until satisfied."
