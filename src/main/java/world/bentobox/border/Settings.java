package world.bentobox.border;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;

import java.util.HashSet;
import java.util.Set;

@StoreAt(filename = "config.yml", path = "addons/Border")
public class Settings implements ConfigObject {

    @ConfigComment("Border addon configuration file")
    @ConfigComment("See the documentation at https://docs.bentobox.world/en/latest/addons/Border/")
    @ConfigComment("")
    @ConfigComment("This list stores GameModes in which Border addon should not work.")
    @ConfigComment("To disable addon it is necessary to write its name in new line that starts with -. Example:")
    @ConfigComment("disabled-gamemodes:")
    @ConfigComment(" - BSkyBlock")
    @ConfigEntry(path = "disabled-gamemodes")
    private Set<String> disabledGameModes = new HashSet<>();

    @ConfigComment("")
    @ConfigComment("Use vanilla world border.")
    @ConfigEntry(path = "use-vanilla")
    private boolean useVanilla = true;

    @ConfigComment("")
    @ConfigComment("Teleport players back inside the border if they somehow get outside.")
    @ConfigComment("This will teleport players back inside if they toggle the border with a command.")
    @ConfigEntry(path = "return-teleport")
    private boolean returnTeleport = true;

    @ConfigComment("")
    @ConfigComment("Only applies if vanilla isn't used.")
    @ConfigComment("Use barrier blocks. If false, the border is indicated by particles only.")
    @ConfigEntry(path = "use-barrier-blocks")
    private boolean useBarrierBlocks = true;

    @ConfigComment("")
    @ConfigComment("Default border behavior")
    @ConfigEntry(path = "show-by-default")
    private boolean showByDefault = true;

    @ConfigComment("")
    @ConfigComment("Only applies if vanilla isn't used.")
    @ConfigComment("Show max-protection range border. This is a visual border only and not a barrier.")
    @ConfigEntry(path = "show-max-border")
    private boolean showMaxBorder = true;

    @ConfigComment("")
    @ConfigComment("Only applies if vanilla isn't used.")
    @ConfigComment("Enables/disables all types of wall particles shown by the addon")
    @ConfigEntry(path = "show-particles")
    private boolean showParticles = true;

    /**
     * @param disabledGameModes new disabledGameModes value.
     */
    public void setDisabledGameModes(Set<String> disabledGameModes) {
        this.disabledGameModes = disabledGameModes;
    }

    /**
     * @return disabledGameModes value.
     */
    public Set<String> getDisabledGameModes() {
        return this.disabledGameModes;
    }

    /**
     * @return the useBarrierBlocks
     */
    public boolean isUseBarrierBlocks() {
        return useBarrierBlocks;
    }

    /**
     * @param useBarrierBlocks the useBarrierBlocks to set
     */
    public void setUseBarrierBlocks(boolean useBarrierBlocks) {
        this.useBarrierBlocks = useBarrierBlocks;
    }

    /**
     * @return the showByDefault
     */
    public boolean isShowByDefault() {
        return showByDefault;
    }

    /**
     * @param showByDefault the showByDefault to set
     */
    public void setShowByDefault(boolean showByDefault) {
        this.showByDefault = showByDefault;
    }

    /**
     * @return the showMaxBorder
     */
    public boolean isShowMaxBorder() {
        return showMaxBorder;
    }

    /**
     * @param showMaxBorder the showMaxBorder to set
     */
    public void setShowMaxBorder(boolean showMaxBorder) {
        this.showMaxBorder = showMaxBorder;
    }

    /**
     * @return the useVanilla
     */
    public boolean isUseVanilla() {
        return useVanilla;
    }

    /**
     * @param useVanilla the useVanilla to set
     */
    public void setUseVanilla(boolean useVanilla) {
        this.useVanilla = useVanilla;
    }

    /**
     * @return the returnTeleport
     */
    public boolean isReturnTeleport() {
        return returnTeleport;
    }

    /**
     * @param returnTeleport the returnTeleport to set
     */
    public void setReturnTeleport(boolean returnTeleport) {
        this.returnTeleport = returnTeleport;
    }

    /**
     * @return the showParticles
     */
    public boolean isShowParticles() {
        return showParticles;
    }

    /**
     * @param showParticles the showParticles to set
     */
    public void setShowParticles(boolean showParticles) {
        this.showParticles = showParticles;
    }
}
