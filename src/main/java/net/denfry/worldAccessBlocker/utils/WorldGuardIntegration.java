package net.denfry.worldAccessBlocker.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;

public class WorldGuardIntegration {
    public static StateFlag WAB_BLOCK;
    public static StateFlag WAB_ALLOW;
    public static StateFlag WAB_SEMI;

    public WorldGuardIntegration(WorldAccessBlocker plugin) {
    }

    public void registerFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("wab-blocked", false);
            StateFlag allow = new StateFlag("wab-allowed", false);
            StateFlag semi = new StateFlag("wab-semi", false);
            registry.register(flag);
            registry.register(allow);
            registry.register(semi);
            WAB_BLOCK = flag;
            WAB_ALLOW = allow;
            WAB_SEMI = semi;
        } catch (FlagConflictException e) {
            var existing = registry.get("wab-blocked");
            if (existing instanceof StateFlag stateFlag) {
                WAB_BLOCK = stateFlag;
            }
            var allow = registry.get("wab-allowed");
            if (allow instanceof StateFlag stateFlag) {
                WAB_ALLOW = stateFlag;
            }
            var semi = registry.get("wab-semi");
            if (semi instanceof StateFlag stateFlag) {
                WAB_SEMI = stateFlag;
            }
        }
    }

    public BlockDecision check(Player player, Location location) {
        if (location == null || WAB_BLOCK == null) return BlockDecision.ALLOW;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return BlockDecision.ALLOW;
        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        if (set == null) return BlockDecision.ALLOW;
        var wrapped = WorldGuardPlugin.inst().wrapPlayer(player);
        boolean allow = WAB_ALLOW != null && set.testState(wrapped, WAB_ALLOW);
        boolean semi = WAB_SEMI != null && set.testState(wrapped, WAB_SEMI);
        boolean blocked = set.testState(wrapped, WAB_BLOCK);
        if (allow) return BlockDecision.ALLOW;
        if (blocked) return semi ? BlockDecision.SEMI : BlockDecision.BLOCK;
        return BlockDecision.ALLOW;
    }

    public Set<ProtectedRegion> getRegions(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return Set.of();
        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return set == null ? Set.of() : set.getRegions();
    }

    public enum BlockDecision {
        BLOCK,
        SEMI,
        ALLOW
    }
}

