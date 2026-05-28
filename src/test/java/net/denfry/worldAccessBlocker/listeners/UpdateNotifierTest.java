package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.utils.LanguageManager;
import net.denfry.worldAccessBlocker.utils.VersionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateNotifierTest {

    private Player player;
    private PlayerJoinEvent event;
    private VersionChecker versionChecker;
    private LanguageManager languageManager;
    private UpdateNotifier notifier;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        versionChecker = mock(VersionChecker.class);
        languageManager = mock(LanguageManager.class);
        when(languageManager.getMessage(eq("update_available"), any()))
                .thenReturn("[WAB] New version 0.8.0 available!");

        notifier = new UpdateNotifier(versionChecker, languageManager);
    }

    @Test
    void sendsMessageWhenNewVersionAndHasPermission() {
        when(versionChecker.getLatestVersion()).thenReturn("0.8.0");
        when(player.hasPermission("wab.update-notify")).thenReturn(true);

        notifier.onPlayerJoin(event);

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(player).sendMessage(captor.capture());
        String plain = PlainTextComponentSerializer.plainText().serialize(captor.getValue());
        assertTrue(plain.contains("0.8.0"), "Message should contain the version number");
    }

    @Test
    void doesNotSendWhenNoPermission() {
        when(versionChecker.getLatestVersion()).thenReturn("0.8.0");
        when(player.hasPermission("wab.update-notify")).thenReturn(false);

        notifier.onPlayerJoin(event);

        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void doesNotSendWhenUpToDate() {
        when(versionChecker.getLatestVersion()).thenReturn("");
        when(player.hasPermission("wab.update-notify")).thenReturn(true);

        notifier.onPlayerJoin(event);

        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void doesNotSendWhenCheckPending() {
        when(versionChecker.getLatestVersion()).thenReturn(null);
        when(player.hasPermission("wab.update-notify")).thenReturn(true);

        notifier.onPlayerJoin(event);

        verify(player, never()).sendMessage(any(Component.class));
    }
}
