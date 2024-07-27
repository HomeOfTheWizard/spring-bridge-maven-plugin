package com.homeofthewizard.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import com.homeofthewizard.friends.MyHelloer;
import com.homeofthewizard.dinerlib.Diner;

import javax.inject.Inject;

@Mojo(name = "meeting", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MeetingMojo extends AbstractMojo {

    private final MyHelloer myFriend;
    private final Diner diner;

    @Inject
    public MeetingMojo(MyHelloer myFriend, Diner diner) {

        this.myFriend = myFriend;
        this.diner = diner;

    }

    public void execute() {

        myFriend.hello();
        diner.eat();

    }
}
