#! /bin/bash
java -cp "./MarkdownGenerator-1.0.6-jar-with-dependencies.jar" com.mdfromhtml.transform.GetTextFromMarkdown "./src/test/resources" "./src/test/resources" "n"
ls -l ./src/test/resources
echo "Now load the .txt file from the ./data/txt directory in an editor or in Chrome"
