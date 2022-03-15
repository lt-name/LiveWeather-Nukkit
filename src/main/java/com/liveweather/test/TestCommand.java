package com.liveweather.test;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.level.WeatherChangeEvent;
import cn.nukkit.event.level.WeatherEvent;
import cn.nukkit.level.Level;
import cn.nukkit.network.protocol.LevelEventPacket;
import com.liveweather.storage.PlayerConfigs;
import ru.nukkitx.forms.elements.CustomForm;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class TestCommand extends Command {
    public TestCommand(String name, String description) {
        super(name, description);
    }
    Boolean raining = false;
    String playername = "";
    int raintime = 0;
    public void setRainTime(int rainTime) {
        raintime = rainTime;
    }
    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if(commandSender instanceof Player) {
            Player p = (Player) commandSender;
            CustomForm form = new CustomForm()
                    .addInput("Enter City");
            form.setTitle("Enter the City you live in");
            form.send(p, (targetPlayer, targetForm, data) -> {
                if(data == null) return; //Если форма закрыта принудительно, то data будет иметь значение null
                //new PlayerConfigs().writeConfig(event.getPlayer().getName(), targetForm.getElements().toString());
                if(data.toString().replace(" ", "").equals("[]")) {

                }else{
                    if(data.toString().contains("")) {
                        return;
                    }
                }
                new PlayerConfigs().writeConfig(p.getName(), data.toString().replace("[", "").replace("]", ""));
                targetPlayer.sendMessage(data.toString());
            });
        }
        return false;
    }
}
