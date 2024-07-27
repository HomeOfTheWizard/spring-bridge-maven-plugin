---
layout: page
title: configuration
nav_order: 2
---

# How to configure it

In the [quick start](/spring-bridge-maven-plugin/#how-to-use-it-) section, we have shown the minimum configuration required for the plugin to help you provide your spring beans (giving the spring configuration class).  

This configuration uses some default values for other configuration parameters that exists.  
Below is the list of properties the plugin takes as parameter for its configuration.
1. `<contextConfigClasses>`: List of configuration classes to use for building the spring context.
2. `<componentClassesFilter>`: List of beans to be created from the configuration class provided.  
   If you do not need all the beans created by the configuration file provided, this can be helpful to reduce the time to build your project, and also the resulting package size.
3. `<applicationPropertiesFile>`: Application properties file path.  
   Default value is `${project.basedir}/src/main/resources/application.properties`.
4. `<outputDir>`: The place where the plugin will generate the source files of the spring context and beans required.  
   Default value is `${project.build.directory}/generated-sources`.

Below is an example of full configuration you can find in the [integration tests](https://github.com/HomeOfTheWizard/spring-bridge-maven-plugin/blob/main/src/it/example/plugin/pom.xml) of the project.
```xml
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
            <contextConfigClass>com.homeofthewizard.friends.MySpringConfiguration</contextConfigClass>
            <contextConfigClass>com.homeofthewizard.dinerlib.DinerSpringConfiguration</contextConfigClass>
        </contextConfigClasses>
        <componentClassesFilter>
            <componentClassesFilter>com.homeofthewizard.friends.MyFriend</componentClassesFilter>
            <componentClassesFilter>com.homeofthewizard.dinerlib.Pizza</componentClassesFilter>
        </componentClassesFilter>
        <applicationPropertiesFile>${project.basedir}/src/main/resources/application.properties</applicationPropertiesFile>
        <outputDir>${project.build.directory}/generated-spring-sources</outputDir>
    </configuration>
</plugin>
```

The beans created by the configuration above can be accessed by a Mojo like below:
```java
@Mojo(name = "meeting", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MeetingMojo extends AbstractMojo {

    private final MyFriend myFriend;
    private final Pizza diner;

    @Inject
    public MeetingMojo(MyFriend myFriend, Pizza diner) {

        this.myFriend = myFriend;
        this.diner = diner;

    }

    public void execute() {

        myFriend.hello();
        diner.eat();

    }
}
```

:warning: As you can see from above the beans can be accessed only using implementation classes. Bean injection by interface type is not yet possible.