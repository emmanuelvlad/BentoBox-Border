package world.bentobox.border.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.border.Border;
import world.bentobox.border.listeners.BorderShower;

public class IslandBorderCommand extends CompositeCommand {

    private Border addon;
    private Island island;

    public IslandBorderCommand(Border addon, CompositeCommand parent, String label) {
        super(addon, parent, label);
        this.addon = addon;
    }

    @Override
    public void setup() {
        this.setPermission("border.toggle");
        this.setDescription("border.toggle.description");
        this.setOnlyPlayer(true);
        setConfigurableRankCommand();

        new BorderTypeCommand(this.getAddon(), this);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!this.getWorld().equals(Util.getWorld(user.getWorld())))
        {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }

        island = getIslands().getIsland(getWorld(), user);
        return island != null;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        boolean on = user.getMetaData(BorderShower.BORDER_STATE_META_DATA).map(MetaDataValue::asBoolean).orElse(addon.getSettings().isShowByDefault());
        if (on) {
            user.sendMessage("border.toggle.border-off");
            user.putMetaData(BorderShower.BORDER_STATE_META_DATA, new MetaDataValue(false));
            addon.getBorderShower().hideBorder(user);
        } else {
            user.sendMessage("border.toggle.border-on");
            user.putMetaData(BorderShower.BORDER_STATE_META_DATA, new MetaDataValue(true));
            addon.getBorderShower().showBorder(user.getPlayer(), island);
        }
        return true;
    }

}
