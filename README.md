[![Maven Central](https://img.shields.io/maven-central/v/com.homeofthewizard/spring-bridge-maven-plugin?color=orange)](https://search.maven.org/artifact/com.homeofthewizard/spring-bridge-maven-plugin)
[![Reproducible Builds](https://img.shields.io/badge/Reproducible_Builds-ok-green?labelColor=blue)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/com/homeofthewizard/spring-bridge-maven-plugin/README.md)
![CI Passing](https://github.com/homeofthewizard/spring-bridge-maven-plugin/actions/workflows/maven-build.yml/badge.svg)

# Spring Bridge Maven Plugin
A plugin that helps other plugins/extensions to use spring libraries.  
It builds the spring context for you at compile time and inject your beans into maven at runtime so you can use them in your Mojos.

# Documentation
You need to add this plugin in the pom.xml of your 'under development' plugin. Like so:
```xml
<dependencies>
    <!-- HERE -->
    <dependency>
        <groupId>com.homeofthewizard</groupId>
        <artifactId>spring-bridge-maven-plugin</artifactId>
        <version>1.0.0-alpha</version>
    </dependency>
    
    <!-- some spring library I want to use in my plugin's mojo -->
    <dependency>
        <groupId>org.springframework.foo</groupId>
        <artifactId>spring-bar-lib</artifactId>
        <version>x.x.x</version>
    </dependency>
</dependencies>

...

<build>
    <plugins>
        <!-- HERE -->
        <plugin>
            <groupId>com.homeofthewizard</groupId>
            <artifactId>spring-bridge-maven-plugin</artifactId>
            <version>1.0.0-alpha</version>
            <executions>
                <execution>
                    <id>generate</id>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <contextConfigClasses>
                    <contextConfigClass>org.springframework.foo.bar.config.SomeBeansConfiguration</contextConfigClass>
                </contextConfigClasses>
            </configuration>
        </plugin>
    </plugins>
</build>
```
You can now use the beans created directly in your Mojo !  
More information can be found [here](https://homeofthewizard.github.io/spring-bridge-maven-plugin/).

# Quick building
You will need Java 17+. Then simply run:  
`mvnw clean package`

# Contributing
* Fork it 
* Create your feature branch (git checkout -b my-new-feature)
* Commit your changes (git commit -am 'Add some feature')
* Push to the branch (git push origin my-new-feature)
* Create new Pull Request
