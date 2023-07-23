package com.homeofthewizard;

import com.homeofthewizard.mocks.TestConfig;
import com.homeofthewizard.mocks.TestPojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

public class SpringHelperTest {

    @Test
    public void should_get_all_classes_from_configs() throws MojoExecutionException {
        var springHelper = new SpringHelper();

        var classes = springHelper.buildSpringContextAndGetBeanClasses(List.of(TestConfig.class),List.of(),List.of("com.homeofthewizard"), Path.of("nada").toFile());

        assert classes.contains(TestPojo.class);
    }
}

