This project is a place to store common Java code useful across other projects in the Platform.

Use the maven build options: clean install to build the jar file.

This project is dependent on an open source JSON library called API4JSON which is accessible from the Maven Central repository. You can add the following to your settings.xml file in ~/.m2 to access this repository in the profiles section:

```
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
```

