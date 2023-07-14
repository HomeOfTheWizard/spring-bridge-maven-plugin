package com.homeofthewizard;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class ProviderGeneratorMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "generated.providers.source.path")
    private File generatedProvidersSourcePath;

    @Parameter(defaultValue = "${project.build.outputDirectory}", property = "generated.providers.output.path")
    private File generatedProvidersClassPath;

    @Parameter(property = "componentClasses")
    private String[] componentClassesFilter = new String[0];

    @Parameter(required = true, property = "contextConfigClasses")
    private String[] contextConfigClasses;

    @Parameter(property = "applicationPropertiesFile", defaultValue = "${project.basedir}/src/main/resources/application.properties")
    private File applicationPropertiesFile;

    @Override
    public void execute() throws MojoExecutionException{
        setClassPath();

        var basePackages = project.getDependencies().stream().map(Dependency::getGroupId).collect(Collectors.toList());
        var configClasses = Arrays.stream(contextConfigClasses).map(this::getClassFromName).collect(Collectors.toList());
        var beanClassesFilter = Arrays.stream(componentClassesFilter).map(this::getClassFromName).collect(Collectors.toList());

        var allBeanClasses = buildSpringContextAndGetBeanClasses(configClasses, applicationPropertiesFile, beanClassesFilter, basePackages);

        generateProvidersForBeans(allBeanClasses);
        try {
            generateSpringConfig(configClasses, applicationPropertiesFile);
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }
        try {
            generateProviderForSpringContext(configClasses);
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }

    }

    private void generateSpringConfig(List<? extends Class<?>> configClasses, File applicationPropertiesFile) throws DependencyResolutionRequiredException {
        ProviderGenerator.generateSpringConfig(
                generatedProvidersSourcePath.toPath(),
//                generatedProvidersClassPath.toPath(),
                project.getGroupId(),
                configClasses,
                applicationPropertiesFile,
                project.getRuntimeClasspathElements()
        );
    }

    private void generateProviderForSpringContext(List<? extends Class<?>> configClasses) throws DependencyResolutionRequiredException {
        ProviderGenerator.generateSpringContextProvider(
                generatedProvidersSourcePath.toPath(),
//                generatedProvidersClassPath.toPath(),
                project.getGroupId(),
                configClasses,
                project.getRuntimeClasspathElements()
        );
    }

    private void generateProvidersForBeans(List<Class<?>> allBeanClasses) {
        allBeanClasses.stream()
                .forEach( beanClass -> {
                    try {
                        ProviderGenerator.generateBeansProviders(
//                                generatedProvidersClassPath.toPath(),
                                generatedProvidersSourcePath.toPath(),
                                project.getGroupId(),
                                beanClass,
                                project.getRuntimeClasspathElements()
                        );
                    } catch (DependencyResolutionRequiredException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private List<Class<?>> buildSpringContextAndGetBeanClasses(List<? extends Class<?>> configClasses, File applicationPropertiesFile, List<? extends Class<?>> beanClassesFilter, List<String> basePackages) throws MojoExecutionException {
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
                .filter( beanClass -> isPartOfLibrary(beanClass, basePackages) && passThroughFilters(beanClass, beanClassesFilter) )
                .collect(Collectors.toList());
    }

    private boolean passThroughFilters(Class<?> beanClass, List<? extends Class<?>> beanClassesFilter) {
        if (!Modifier.isPublic(beanClass.getModifiers())) return false;
        if (beanClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) return false;
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
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void setClassPath(){
        try {
            Set<URL> urls = new HashSet<>();
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

        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
