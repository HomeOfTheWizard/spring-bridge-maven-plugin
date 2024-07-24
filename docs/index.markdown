---
layout: page
title: About the plugin
nav_order: 1
---

# Use spring libraries freely in your maven plugins/extensions

[![Quick Start](https://img.shields.io/badge/-Quick%20Start%20%F0%9F%9A%80-blue?style=for-the-badge&logo=rocket)](#how-to-use-it-)
[![Github button](https://img.shields.io/badge/-View%20it%20on%20Github-gray?style=for-the-badge&logo=github)](https://github.com/HomeOfTheWizard/spring-bridge-maven-plugin)
[![Maven Central Releases](https://img.shields.io/badge/-Maven%20Releases-orange?style=for-the-badge&logo=apache%20maven)](https://central.sonatype.com/artifact/com.homeofthewizard/spring-bridge-maven-plugin)
[![JavaDoc](https://img.shields.io/badge/-JavaDocs%F0%9F%93%84-green?style=for-the-badge)](https://www.javadoc.io/doc/com.homeofthewizard/spring-bridge-maven-plugin/latest)

Maven uses [Sisu](https://eclipse.github.io/sisu.inject/) as dependency injection mechanism and this plugin allows you to have a bridge between the two.  
You just add this plugin in your project's pom.xml, configure it, and you will be able to simply inject your beans in your mojo using `@Inject` and start using them.   
This plugin will be executed during the build of your own plugin, in order to generate the Spring Beans you need in your project. So you plugin will be packaged with the bean classes you require. See the [below](#how-it-works-under-the-hood--) section for more details.

---

# How to use it ?
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

⚠️ Pay attention to the fact that we have to provide the plugin in both `dependencies` and `plugin` sections.  
This is because your plugin will also needs some dependencies to run spring, and this plugin has them for you.

The plugin section allows this project to generate code for you, that will be picked up by maven injection mechanism.  
This will be done when you build your plugin.

Then in the `<configuration>` section we tell what to do with this library (spring configuration class, and application properties to use, ect..).  
This is an example that shows the minimum required configuration.  
We only provide the spring ContextConfiguration class that creates the beans we want from the imported library `spring-some-lib`.  
An example with full configuration can be found in the integration tests [here](./src/it/example/plugin/pom.xml)

#### This is it! you are ready to go :rocket:
You can now use the beans created directly in your Mojo.

```java
import org.springframework.foo.bar.SomeClass;

@Mojo(name = "run")
public class MyMojo extends AbstractMojo
{
    private final SomeClass someInstance;

    @Inject
    public MyMojo(SomeClass someInstance) {
        this.someInstance = someInstance;
    }

    public void execute()
    {
        // Using the spring bean injected into your mojo
        someInstance.doSomething();
    }
}
```

---

# How it works under the hood ?
This plugin allows you to use JSR330 annotations to inject your spring beans.  
JSR330 is made available in maven with the arrival of sisu.   
Maven uses Sisu since version 3.1.0, so this plugin works from Maven 3.1.x and on. It requires Java 11 +.    
See this [doc](https://maven.apache.org/maven-jsr330.html) for more details on how to use JSR330 in maven plugins and about maven's history of DI mechanism.

The plugin allows injecting the spring beans into your Mojo by generating code of a JSR330 provider for each bean.   
It works in 3 steps:
1. The sources of your plugin will be compiled on the `COMPILE` phase by the maven compile plugin.
2. The spring-bridge plugin is configurer to run during the same `COMPILE` phase right after the compilation of your code.  
   This is necessary for classpath sharing. In case the spring library or configuration you want to use is not coming from another maven dependency, but from your project's codebase, then spring-bridge-maven-plugin needs to access to your classpath in order to generate the necessary spring context. After running the spring container and construct the beans you need, it generates source code of JSR330 Providers for them, so they will be picked by sisu.
3. Then the plugin will run again the compilation of the whole project with the generated code.

Lastly the classes will be scanned and indexed by the sisu component.

---

# Alternatives ?
There is another way, that does not require compiling code multiple times, which is faster, but have other disadvantages. So it is up to you to judge which one fits better your requirements.  
Maven-Sisu is based on Google Guice and using Guice API we can create custom bindings to be used in the maven DI system. But maven core unfortunately does not expose the Api to plugins by default.  
See the documentation [here](https://eclipse-sisu.github.io/sisu-project/plexus/index.html#custombinding).

So we have two options to bypass this:
1. This plugin,
2. Use an extension to activate GuiceAPI, and use the following library to directly create your beans and inject them into sisu, [here](https://github.com/HomeOfTheWizard/spring-bridge-maven).

Activating the Guice API to all maven plugins via a core extension may be non practical, or not possible at all for some projects.
A [discussion](https://issues.apache.org/jira/browse/MNG-7921) is ongoing to tackle this problem.
