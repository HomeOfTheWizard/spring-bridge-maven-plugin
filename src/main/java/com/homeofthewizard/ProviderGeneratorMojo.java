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

        var allBeanClasses = buildSpringContextAndGetBeanClasses(configClasses, applicationPropertiesFile);

        generateProvidersForBeans(basePackages, beanClassesFilter, allBeanClasses);
        generateSpringConfig(configClasses, applicationPropertiesFile);
        generateProviderForSpringContext(configClasses);

    }

    private void generateSpringConfig(List<? extends Class<?>> configClasses, File applicationPropertiesFile) {
        ProviderGenerator.generateSpringConfig(
                generatedProvidersSourcePath.toPath(),
                project.getGroupId(),
                configClasses,
                applicationPropertiesFile
        );
    }

    private void generateProviderForSpringContext(List<? extends Class<?>> configClasses) {
        ProviderGenerator.generateSpringContextProvider(
                generatedProvidersSourcePath.toPath(),
                project.getGroupId(),
                configClasses
        );
    }

    private void generateProvidersForBeans(List<String> basePackages, List<? extends Class<?>> beanClassesFilter, List<Class<?>> allBeanClasses) {
        allBeanClasses.stream()
                .filter( beanClass -> isPartOfLibrary(beanClass, basePackages) && passThroughFilters(beanClass, beanClassesFilter) )
                .forEach( beanClass -> ProviderGenerator.generateBeansProviders(
                        generatedProvidersSourcePath.toPath(),
                        project.getGroupId(),
                        beanClass
                ));
    }

    private List<Class<?>> buildSpringContextAndGetBeanClasses(List<? extends Class<?>> configClasses, File applicationPropertiesFile) throws MojoExecutionException {
        var resourcePropertyFile = createResourceFromPropertyFile(applicationPropertiesFile);
        var applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.getEnvironment().getPropertySources().addFirst(resourcePropertyFile);
        applicationContext.register(configClasses.toArray(new Class[0]));
        applicationContext.refresh();

        return Arrays.stream(applicationContext.getBeanDefinitionNames())
                .map(applicationContext::getBean)
                .map(Object::getClass)
                .collect(Collectors.toList());
    }

    private boolean passThroughFilters(Class<?> beanClass, List<? extends Class<?>> beanClassesFilter) {
        if (!Modifier.isPublic(beanClass.getModifiers())) return false;
        if (beanClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) return false;
        if (!beanClassesFilter.isEmpty()) return beanClassesFilter.contains(beanClass);
        return true;
    }

    private boolean isPartOfLibrary(Class<?> beanClass, List<String> basePackages) {
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
