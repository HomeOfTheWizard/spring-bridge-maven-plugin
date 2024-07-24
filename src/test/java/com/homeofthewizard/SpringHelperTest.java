package com.homeofthewizard;

import com.homeofthewizard.mocks.TestConfig;
import com.homeofthewizard.mocks.TestFilterPojo;
import com.homeofthewizard.mocks.TestPojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class SpringHelperTest {

    @Test
    public void should_get_all_classes_from_configs() throws MojoExecutionException {
        var springHelper = new SpringHelper();

        var classes = springHelper.buildSpringContextAndGetBeanClasses(List.of(TestConfig.class), List.of(), List.of("com.homeofthewizard"), Path.of("nada").toFile());

        assert classes.contains(TestPojo.class);
        assert classes.contains(TestFilterPojo.class);
    }

    @Test
    public void should_get_only_classes_from_configs() throws MojoExecutionException {
        var springHelper = new SpringHelper();

        var classes = springHelper.buildSpringContextAndGetBeanClasses(List.of(TestConfig.class),List.of(TestPojo.class),List.of("com.homeofthewizard"), Path.of("nada").toFile());

        assert classes.contains(TestPojo.class);
        assert !classes.contains(TestFilterPojo.class);
    }

    @Test
    public void should_get_only_classes_from_dependency_libraries() throws MojoExecutionException {
        var springHelper = new SpringHelper();

        var classes = springHelper.buildSpringContextAndGetBeanClasses(List.of(TestConfig.class), List.of(), List.of("com.homeofthewizard"), Path.of("nada").toFile());

        assert !classes.contains(ConfigurationClassPostProcessor.class);
        assert classes.stream().allMatch(clazz -> clazz.getPackageName().contains("com.homeofthewizard"));
    }

    @Test
    public void should_get_throw_if_basePackage_is_empty() throws MojoExecutionException {
        var springHelper = new SpringHelper();

        assertThrowsExactly(IllegalArgumentException.class,
                () -> springHelper.buildSpringContextAndGetBeanClasses(List.of(TestConfig.class), List.of(), List.of(), Path.of("nada").toFile()));
    }
}

