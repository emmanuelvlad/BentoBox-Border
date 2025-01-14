package world.bentobox.border.listeners;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;
import world.bentobox.bentobox.api.events.island.IslandProtectionRangeChangeEvent;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.border.Border;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * @author tastybento
 */
public class PlayerListener implements Listener {

    private static final Vector XZ = new Vector(1,0,1);
    private final Border addon;
    private Set<UUID> inTeleport;
    private final BorderShower show;
    private Map<Player, BukkitTask> mountedPlayers = new HashMap<>();

    public PlayerListener(Border addon) {
        this.addon = addon;
        inTeleport = new HashSet<>();
        this.show = addon.getBorderShower();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent e) {
        show.clearUser(User.getInstance(e.getPlayer()));
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> addon.getIslands().getIslandAt(e.getPlayer().getLocation()).ifPresent(i ->
        show.showBorder(e.getPlayer(), i)));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent e) {
        show.clearUser(User.getInstance(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        show.clearUser(User.getInstance(e.getPlayer()));
        Bukkit.getScheduler().runTask(addon.getPlugin(), () -> addon.getIslands().getIslandAt(e.getPlayer().getLocation()).ifPresent(i ->
        show.showBorder(e.getPlayer(), i)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        Location to = e.getTo();

        show.clearUser(User.getInstance(player));

        if (!addon.inGameWorld(to.getWorld())) {
            return;
        }

        TeleportCause cause = e.getCause();
        boolean isBlacklistedCause = cause == TeleportCause.ENDER_PEARL || cause == TeleportCause.CHORUS_FRUIT;

        Bukkit.getScheduler().runTask(addon.getPlugin(), () ->
            addon.getIslands().getIslandAt(to).ifPresentOrElse(i -> {
                Optional<Flag> boxedEnderPearlFlag = i.getPlugin().getFlagsManager().getFlag("ALLOW_MOVE_BOX");

                if (isBlacklistedCause
                && (!i.getProtectionBoundingBox().contains(to.toVector())
                || !i.onIsland(player.getLocation()))) {
                    e.setCancelled(true);
                }

                if (boxedEnderPearlFlag.isPresent()
                && boxedEnderPearlFlag.get().isSetForWorld(to.getWorld())
                && cause == TeleportCause.ENDER_PEARL) {
                    e.setCancelled(false);
                }

                show.showBorder(player, i);
            }, () -> {
                if (isBlacklistedCause) {
                    e.setCancelled(true);
                    return;
                }
            })
        );
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLeaveIsland(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location from = e.getFrom();
        if (!addon.getSettings().isReturnTeleport() || !outsideCheck(e.getPlayer(), from, e.getTo())) {
            return;
        }
        // Move the player back inside the border
        if (addon.getIslands().getProtectedIslandAt(from).isPresent()) {
            e.setCancelled(true);
            inTeleport.add(p.getUniqueId());
            Util.teleportAsync(p, from).thenRun(() -> inTeleport.remove(p.getUniqueId()));
            return;
        }
        // Backtrack
        addon.getIslands().getIslandAt(p.getLocation()).ifPresent(i -> {
            Vector unitVector = i.getProtectionCenter().toVector().subtract(p.getLocation().toVector()).normalize()
                    .multiply(new Vector(1,0,1));
            RayTraceResult r = i.getProtectionBoundingBox().rayTrace(p.getLocation().toVector(), unitVector, i.getRange());
            if (r != null) {
                inTeleport.add(p.getUniqueId());
                Location targetPos = r.getHitPosition().toLocation(p.getWorld(), p.getLocation().getYaw(), p.getLocation().getPitch());
                if (!addon.getIslands().isSafeLocation(targetPos)) {
                    switch (targetPos.getWorld().getEnvironment()) {
                    case NETHER:
                        targetPos.getBlock().getRelative(BlockFace.DOWN).setType(Material.NETHERRACK);
                        break;
                    case THE_END:
                        targetPos.getBlock().getRelative(BlockFace.DOWN).setType(Material.END_STONE);
                        break;
                    default:
                        targetPos.getBlock().getRelative(BlockFace.DOWN).setType(Material.STONE);
                        break;
                    }
                }
                Util.teleportAsync(p, targetPos).thenRun(() -> inTeleport.remove(p.getUniqueId()));
            }
        });
    }

    /**
     * Check if the player is outside the island protection zone that they are supposed to be in.
     * @param player - player moving
     * @param from - from location
     * @param to - to location
     * @return true if outside the island protection zone
     */
    private boolean outsideCheck(Player player, Location from, Location to) {
        User user = Objects.requireNonNull(User.getInstance(player));

        if ((from.getWorld() != null && from.getWorld().equals(to.getWorld())
                && from.toVector().multiply(XZ).equals(to.toVector().multiply(XZ)))
                || !addon.inGameWorld(player.getWorld())
                || user.getPlayer().getGameMode() == GameMode.SPECTATOR
                // || !addon.getIslands().getIslandAt(to).filter(i -> addon.getIslands().locationIsOnIsland(player, i.getProtectionCenter())).isPresent()
                || !user.getMetaData(BorderShower.BORDER_STATE_META_DATA).map(MetaDataValue::asBoolean).orElse(addon.getSettings().isShowByDefault())) {
            return false;
        }
        return addon.getIslands().getIslandAt(to).filter(i -> !i.onIsland(to)).isPresent();
    }

    /**
     * Runs a task while the player is mounting an entity and eject
     * if the entity went outside the protection range
     * @param event - event
     */
    @EventHandler
    public void onEntityMount(EntityMountEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        mountedPlayers.put(player, Bukkit.getScheduler().runTaskTimer(addon.getPlugin(), () -> {
            Location loc = player.getLocation();

            if (!addon.inGameWorld(loc.getWorld())) {
                return;
            }
            // Eject from mount if outside the protection range
            if (addon.getIslands().getProtectedIslandAt(loc).isEmpty()) {
                // Force the dismount event for custom entities
                if (!event.getMount().eject()) {
                    var dismountEvent = new EntityDismountEvent(player, event.getMount());
                    Bukkit.getPluginManager().callEvent(dismountEvent);
                }
            }
        }, 1, 20));
    }

    /**
     * Cancel the running task if the player was mounting an entity
     * @param event - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        BukkitTask task = mountedPlayers.get(player);
        if (task == null) {
            return;
        }

        task.cancel();
        mountedPlayers.remove(player);
    }

    /**
     * Teleports a player back home if they use a vehicle to glitch out of the world border
     * @param event - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDismount(VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            Player p = (Player) event.getExited();
            if (p.hasPermission(addon.getPermissionPrefix() + "border.on")) {
                Optional<Island> is = addon.getIslands().getProtectedIslandAt(p.getLocation());
                if (is.isPresent()) {
                    Bukkit.getScheduler().runTask(addon.getPlugin(), () -> {
                        if (!addon.getIslands().getProtectedIslandAt(p.getLocation()).isPresent()
                                && addon.getIslands().getIslandAt(p.getLocation()).equals(is)) {
                            addon.getIslands().homeTeleportAsync(Util.getWorld(p.getWorld()), p);
                        }
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent e) {
        // Remove head movement
        if (!e.getFrom().toVector().equals(e.getTo().toVector())) {
            addon.getIslands()
                    .getIslandAt(e.getPlayer().getLocation())
                    .ifPresent(i -> show.refreshView(User.getInstance(e.getPlayer()), i));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVehicleMove(VehicleMoveEvent e) {
        // Remove head movement
        if (!e.getFrom().toVector().equals(e.getTo().toVector())) {
            e.getVehicle().getPassengers().stream()
                    .filter(Player.class::isInstance)
                    .map(Player.class::cast)
                    .forEach(p -> addon
                            .getIslands()
                            .getIslandAt(p.getLocation())
                            .ifPresent(i -> show.refreshView(User.getInstance(p), i)));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProtectionRangeChange(IslandProtectionRangeChangeEvent e) {
        // Hide and show again
        e.getIsland().getPlayersOnIsland().forEach(player -> {
            show.hideBorder(User.getInstance(player));
            show.showBorder(player, e.getIsland());
        });
    }
}
