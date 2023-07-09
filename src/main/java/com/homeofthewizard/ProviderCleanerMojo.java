package com.homeofthewizard;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo( name = "clean", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class ProviderCleanerMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

    }
}
