package com.homeofthewizard.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import com.homeofthewizard.friends.MyFriend;
import com.homeofthewizard.dinerlib.Pizza;

import javax.inject.Inject;

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
