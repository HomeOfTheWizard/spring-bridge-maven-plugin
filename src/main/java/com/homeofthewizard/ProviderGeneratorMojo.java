package com.homeofthewizard;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ClassUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.TEST)
public class ProviderGeneratorMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Inject
    private BuildPluginManager pluginManager;

    @Parameter(property = "componentClasses")
    private String[] componentClassesFilter = new String[0];

    @Parameter(required = true, property = "contextConfigClasses")
    private String[] contextConfigClasses;

    @Parameter(property = "applicationPropertiesFile", defaultValue = "${project.basedir}/src/main/resources/application.properties")
    private File applicationPropertiesFile;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources", required = true)
    private File outputDir;

    @Override
    public void execute() throws MojoExecutionException{
        setClassPath();

        var basePackages = project.getDependencies().stream().map(Dependency::getGroupId).collect(Collectors.toList());
        var configClasses = Arrays.stream(contextConfigClasses).map(this::getClassFromName).collect(Collectors.toList());
        var beanClassesFilter = Arrays.stream(componentClassesFilter).map(this::getClassFromName).collect(Collectors.toList());

        var beanClasses = buildSpringContextAndGetBeanClasses(configClasses, beanClassesFilter, basePackages, applicationPropertiesFile);

        generateProvidersForBeans(beanClasses);
        generateSpringConfig(configClasses, applicationPropertiesFile);
        generateProviderForSpringContext(configClasses);

        compileGeneratedClasses();

    }

    private void setClassPath() throws MojoExecutionException {
        try {
            var urls = new HashSet<URL>();
            List<String> elements = project.getRuntimeClasspathElements();
            elements.addAll(project.getCompileClasspathElements());
            elements.addAll(project.getTestClasspathElements());

            for (String element : elements) {
                urls.add(new File(element).toURI().toURL());
            }

            ClassLoader contextClassLoader = URLClassLoader.newInstance(
                    urls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader());

            Thread.currentThread().setContextClassLoader(contextClassLoader);

        } catch (DependencyResolutionRequiredException | MalformedURLException e) {
            throw new MojoExecutionException("Could not merge classpath of plugin and project", e);
        }
    }

    private Class<?> getClassFromName(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateProvidersForBeans(List<Class<?>> allBeanClasses) {
        for(var bean : allBeanClasses){
            ProviderGenerator.generateBeansProviders(outputDir.toPath(), project.getGroupId(), bean);
        }
    }

    private void generateSpringConfig(List<? extends Class<?>> configClasses, File applicationPropertiesFile) {
        ProviderGenerator.generateSpringConfig(
                outputDir.toPath(),
                project.getGroupId(),
                configClasses,
                applicationPropertiesFile
        );
    }

    private void generateProviderForSpringContext(List<? extends Class<?>> configClasses) {
        ProviderGenerator.generateSpringContextProvider(
                outputDir.toPath(),
                project.getGroupId(),
                configClasses
        );
    }

    private List<Class<?>> buildSpringContextAndGetBeanClasses(List<? extends Class<?>> configClasses, List<? extends Class<?>> beanClassesFilter, List<String> basePackages, File applicationPropertiesFile) throws MojoExecutionException {
        var applicationContext = new AnnotationConfigApplicationContext();
        if(applicationPropertiesFile.exists()) {
            var resourcePropertyFile = createResourceFromPropertyFile(applicationPropertiesFile);
            applicationContext.getEnvironment().getPropertySources().addFirst(resourcePropertyFile);
        }
        applicationContext.register(configClasses.toArray(new Class[0]));
        applicationContext.refresh();

        var beansStream = beanClassesFilter.isEmpty()
                ? Arrays.stream(applicationContext.getBeanDefinitionNames()).map(applicationContext::getBean)
                : beanClassesFilter.stream().map(applicationContext::getBean);

        return beansStream
                .map(Object::getClass)
                .filter( beanClass -> isPartOfLibrary(beanClass, basePackages) && passThroughFilters(beanClass) )
                .collect(Collectors.toList());
    }

    private ResourcePropertySource createResourceFromPropertyFile(File applicationPropertiesFile) throws MojoExecutionException {
        try {
            return new ResourcePropertySource( new FileSystemResource( applicationPropertiesFile ) );
        } catch (IOException e) {
            throw new MojoExecutionException("Application property file could not be opened",e);
        }
    }

    private boolean passThroughFilters(Class<?> beanClass) {
        if (!Modifier.isPublic(beanClass.getModifiers())) return false;
        if (beanClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) return false;
        return true;
    }

    private boolean isPartOfLibrary(Class<?> beanClass, List<String> basePackages) {
        return basePackages.stream().anyMatch(bPackage -> beanClass.getPackage().toString().contains(bPackage) );
    }

    private void compileGeneratedClasses() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-compiler-plugin"),
                        version("3.3")
                ),
                goal("compile"),
                configuration(
                        element(name("generatedSourcesDirectory"), "${project.build.directory}/generated-sources")
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
    }
}
