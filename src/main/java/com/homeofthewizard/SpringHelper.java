package com.homeofthewizard;

import org.apache.maven.plugin.MojoExecutionException;
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

public class SpringHelper {

    public List<Class<?>> buildSpringContextAndGetBeanClasses(List<? extends Class<?>> configClasses, List<? extends Class<?>> beanClassesFilter, List<String> basePackages, File applicationPropertiesFile) throws MojoExecutionException {
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

}
