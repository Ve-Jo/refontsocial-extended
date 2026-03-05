package ru.rizonchik.refontsocial.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.util.Colors;

public final class SitManager implements Listener {
    private final RefontSocial plugin;
    private final Map<UUID, ArmorStand> seats = new ConcurrentHashMap<UUID, ArmorStand>();
    private final Map<UUID, ArmorStand> laying = new ConcurrentHashMap<UUID, ArmorStand>();

    public SitManager(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public boolean toggleSit(Player player) {
        if (this.isSeated(player)) {
            this.standUp(player, true);
            return false;
        }
        if (!this.canSit(player)) {
            player.sendMessage(Colors.msg(this.plugin, "sitCannot"));
            return false;
        }
        ArmorStand stand = this.spawnSeat(player);
        if (stand == null) {
            player.sendMessage(Colors.msg(this.plugin, "sitCannot"));
            return false;
        }
        this.laying.remove(player.getUniqueId());
        player.setPose(Pose.STANDING);
        stand.addPassenger(player);
        this.seats.put(player.getUniqueId(), stand);
        player.sendMessage(Colors.msg(this.plugin, "sitStart"));
        return true;
    }

    public boolean toggleLay(Player player) {
        if (this.isLaying(player)) {
            this.stopLaying(player, true, true);
            return false;
        }
        if (!this.canLay(player)) {
            player.sendMessage(Colors.msg(this.plugin, "layCannot"));
            return false;
        }
        this.standUp(player, false);
        ArmorStand stand = this.spawnLaySeat(player);
        if (stand == null) {
            player.sendMessage(Colors.msg(this.plugin, "layCannot"));
            return false;
        }
        player.setPose(Pose.SLEEPING);
        stand.addPassenger(player);
        this.laying.put(player.getUniqueId(), stand);
        player.sendMessage(Colors.msg(this.plugin, "layStart"));
        return true;
    }

    public void shutdown() {
        for (UUID uuid : this.seats.keySet()) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            if (player != null) {
                this.standUp(player, false);
            }
        }
        for (UUID uuid : this.laying.keySet()) {
            Player player = this.plugin.getServer().getPlayer(uuid);
            if (player != null) {
                this.stopLaying(player, false, false);
            }
        }
        this.seats.clear();
        this.laying.clear();
    }

    private boolean isSeated(Player player) {
        return this.seats.containsKey(player.getUniqueId());
    }

    private boolean isLaying(Player player) {
        return this.laying.containsKey(player.getUniqueId());
    }

    private boolean canSit(Player player) {
        if (player == null || player.isDead()) {
            return false;
        }
        if (player.isInsideVehicle() || player.isSwimming() || player.isGliding()) {
            return false;
        }
        return this.isSurfaceValid(player);
    }

    private boolean canLay(Player player) {
        if (player == null || player.isDead()) {
            return false;
        }
        if (player.isInsideVehicle() || player.isSwimming() || player.isGliding()) {
            return false;
        }
        return this.isSurfaceValid(player);
    }

    private boolean isSurfaceValid(Player player) {
        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (below.getType() == Material.AIR || below.isEmpty() || below.isLiquid()) {
            return false;
        }
        return below.getType().isSolid();
    }

    private ArmorStand spawnSeat(Player player) {
        Location seatLocation = this.getSeatLocation(player);
        if (seatLocation == null) {
            return null;
        }
        World world = seatLocation.getWorld();
        if (world == null) {
            return null;
        }
        ArmorStand stand = (ArmorStand) world.spawnEntity(seatLocation, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setCanPickupItems(false);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setRotation(player.getLocation().getYaw(), 0.0f);
        return stand;
    }

    private ArmorStand spawnLaySeat(Player player) {
        Location seatLocation = this.getLayLocation(player);
        if (seatLocation == null) {
            return null;
        }
        World world = seatLocation.getWorld();
        if (world == null) {
            return null;
        }
        ArmorStand stand = (ArmorStand) world.spawnEntity(seatLocation, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setSmall(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setCanPickupItems(false);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setRotation(player.getLocation().getYaw(), 0.0f);
        return stand;
    }

    private Location getSeatLocation(Player player) {
        Location loc = player.getLocation();
        Block below = loc.getBlock().getRelative(BlockFace.DOWN);
        if (below.getType() == Material.AIR) {
            return null;
        }
        double y = below.getY() + 1.0;
        BlockData data = below.getBlockData();
        if (data instanceof Slab || data instanceof Stairs) {
            y -= 0.5;
        }
        return new Location(loc.getWorld(), below.getX() + 0.5, y, below.getZ() + 0.5, loc.getYaw(), loc.getPitch());
    }

    private Location getLayLocation(Player player) {
        Location loc = player.getLocation();
        Block below = loc.getBlock().getRelative(BlockFace.DOWN);
        if (below.getType() == Material.AIR) {
            return null;
        }
        double y = below.getY() + 0.2;
        return new Location(loc.getWorld(), below.getX() + 0.5, y, below.getZ() + 0.5, loc.getYaw(), loc.getPitch());
    }

    private void standUp(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        ArmorStand stand = this.seats.remove(uuid);
        if (stand != null) {
            stand.removePassenger(player);
            stand.remove();
        }
        if (notify) {
            player.sendMessage(Colors.msg(this.plugin, "sitStop"));
        }
    }

    private void stopLaying(Player player, boolean notify) {
        this.stopLaying(player, notify, true);
    }

    private void stopLaying(Player player, boolean notify, boolean lift) {
        UUID uuid = player.getUniqueId();
        ArmorStand stand = this.laying.remove(uuid);
        if (stand != null) {
            stand.removePassenger(player);
            stand.remove();
        }
        player.setPose(Pose.STANDING);
        if (lift) {
            Location loc = player.getLocation();
            player.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY() + 1.0, loc.getZ(), loc.getYaw(), loc.getPitch()));
        }
        if (notify) {
            player.sendMessage(Colors.msg(this.plugin, "layStop"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.standUp(player, false);
        this.stopLaying(player, false);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        this.standUp(player, false);
        this.stopLaying(player, false, false);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.isSeated(player)) {
            this.standUp(player, false);
        }
        if (this.isLaying(player)) {
            this.stopLaying(player, false);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (this.isSeated(player)) {
            this.standUp(player, true);
        }
        if (this.isLaying(player)) {
            this.stopLaying(player, true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        this.standUp(player, false);
        this.stopLaying(player, false);
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        Entity entity = event.getExited();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player) entity;
        if (this.isSeated(player)) {
            this.standUp(player, false);
        }
        if (this.isLaying(player)) {
            this.stopLaying(player, false);
        }
    }
}
