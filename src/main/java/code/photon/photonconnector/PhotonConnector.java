package code.photon.photonconnector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class PhotonConnector extends JavaPlugin implements Listener {

    private static String bridgePassword;
    private static int bridgePort;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        bridgePassword = getConfig().getString("bridge-password");
        bridgePort = getConfig().getInt("photon-port");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("server") && sender.hasPermission("photon.server")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: &7/server <server-name>"));
                return true;
            }

            ByteBuf buf = Unpooled.buffer();
            byte[] passwordBytes = bridgePassword.getBytes(StandardCharsets.UTF_8);
            byte[] servernameBytes = args[0].getBytes(StandardCharsets.UTF_8);
            byte[] usernameBytes = sender.getName().getBytes(StandardCharsets.UTF_8);
            byte[] packetNameBytes = "switch_server".getBytes(StandardCharsets.UTF_8);

            // Bridge connect
            buf.writeByte(0);
            buf.writeByte(passwordBytes.length);
            buf.writeBytes(passwordBytes);

            // Packet
            buf.writeByte(packetNameBytes.length);
            buf.writeBytes(packetNameBytes);

            int packetSize = 1 + 1 + usernameBytes.length + servernameBytes.length;
            buf.writeInt(packetSize);

            buf.writeByte(usernameBytes.length);
            buf.writeBytes(usernameBytes);
            buf.writeByte(servernameBytes.length);
            buf.writeBytes(servernameBytes);

            sendMessage((Player) sender, buf.array());
            return true;
        }

        return false;
    }

    public void sendMessage(Player player, byte[] bytes) {
        final EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        InetSocketAddress address = (InetSocketAddress) entityPlayer.playerConnection.networkManager.socketAddress;
        try {
            Socket socket = new Socket(); // No idea how bad this is, but I imagine it is quite bad.
            socket.connect(new InetSocketAddress(address.getAddress(), bridgePort));
            socket.getOutputStream().write(bytes);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
