package com.homeofthewizard;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ProviderGeneratorMojo extends AbstractMojo {

//    private File generatedProvidersBuildPath;
//    private String generatedProvidersPackageName;

//    @Parameter(defaultValue = "${project.build.directory}", required = true, property = "generated.providers.build.path")
//    public void setGeneratedProvidersBuildPath(File generatedClassPath) {
//        this.generatedProvidersBuildPath = generatedClassPath;
//    }

//    @Parameter(defaultValue = "${project.groupId}", required = true, property = "generated.providers.package")
//    public void setGeneratedProvidersPackageName(String packageName) {
//        this.generatedProvidersPackageName = packageName;
//    }

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "generated.providers.source.path")
    private File generatedProvidersSourcePath;

    @Parameter(property = "componentClasses")
    private String[] componentClassesFilter = new String[0];

    @Parameter(required = true, property = "contextConfigClasses")
    private String[] contextConfigClasses;

    @Parameter(required = true, property = "applicationPropertiesFile")
    private File applicationPropertiesFile;

    @Override
    public void execute() throws MojoExecutionException{

        var basePackages = project.getDependencies().stream().map(Dependency::getGroupId).collect(Collectors.toList());

        var configClasses = Arrays.stream(contextConfigClasses).map(this::getClassFromName).collect(Collectors.toList());
        var beanClassesFilter = Arrays.stream(componentClassesFilter).map(this::getClassFromName).collect(Collectors.toList());
        var resourcePropertyFile = createResourceFromPropertyFile(applicationPropertiesFile);

        var applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.getEnvironment().getPropertySources().addFirst(resourcePropertyFile);
        applicationContext.register(configClasses.toArray(new Class[0]));
        applicationContext.refresh();

        List<Class> allBeanClasses = Arrays.stream(applicationContext.getBeanDefinitionNames())
                .map(applicationContext::getBean)
                .map(Object::getClass)
                .collect(Collectors.toList());

        for(Class beanClass : allBeanClasses) {
            if( isPartOfLibrary(beanClass, basePackages) && passThroughFilters(beanClass, beanClassesFilter)) {
                ProviderGenerator.generateBeansProviders(
                        generatedProvidersSourcePath.toPath(),
                        project.getGroupId(),
                        beanClass
                );
            }
        }

        ProviderGenerator.generateSpringContextProvider(
                generatedProvidersSourcePath.toPath(),
                project.getGroupId(),
                configClasses
        );

    }

    private boolean passThroughFilters(Class beanClass, List<? extends Class<?>> beanClassesFilter) {
        if (!Modifier.isPublic(beanClass.getModifiers())) return false;
        if (beanClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) return false;
        if (!beanClassesFilter.isEmpty()) return beanClassesFilter.contains(beanClass);
        return true;
    }

    private boolean isPartOfLibrary(Class beanClass, List<String> basePackages) {
        return basePackages.stream().anyMatch(bPackage -> beanClass.getPackage().toString().contains(bPackage) );
    }

    private ResourcePropertySource createResourceFromPropertyFile(File applicationPropertiesFile) throws MojoExecutionException {
        try {
            return new ResourcePropertySource( new FileSystemResource( applicationPropertiesFile ) );
        } catch (IOException e) {
            throw new MojoExecutionException("",e);
        }
    }

    private Class<?> getClassFromName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
