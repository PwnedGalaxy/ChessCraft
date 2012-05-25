package me.desht.chesscraft.blocks;

import java.io.File;
import java.io.IOException;

import me.desht.chesscraft.ChessCraft;
import me.desht.chesscraft.DirectoryStructure;
import me.desht.chesscraft.Messages;
import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.dhutils.LogUtils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.FilenameException;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;

public class TerrainBackup {

	private Player player;
	private WorldEditPlugin wep;
	private WorldEdit we;
	private LocalSession localSession;
	private EditSession editSession;
	private LocalPlayer localPlayer;
	private CuboidClipboard clipboard;
	private File saveFile;
	private Vector min, max;

	private TerrainBackup(Player player, BoardView view) throws FilenameException {
		this.player = player;

		wep = ChessCraft.getWorldEdit();
		if (wep == null) {
			return;
		}
		we = wep.getWorldEdit();

		Cuboid bounds = view.getOuterBounds();
		Location l1 = bounds.getUpperSW();
		Location l2 = bounds.getLowerNE();
		max = new Vector(l1.getBlockX(), l1.getBlockY(), l1.getBlockZ());
		min = new Vector(l2.getBlockX(), l2.getBlockY(), l2.getBlockZ());

		localPlayer = wep.wrapPlayer(player);
		localSession = we.getSession(localPlayer);
		editSession = localSession.createEditSession(localPlayer);

		saveFile = we.getSafeSaveFile(localPlayer, DirectoryStructure.getSchematicsDirectory(), view.getName(),
		                              "schematic", new String[] { "schematic" }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void saveTerrain() {
		if (wep == null) {
			return;
		}

		try {
			editSession.enableQueue();
			clipboard = new CuboidClipboard(max.subtract(min).add(new Vector(1, 1, 1)), min);
			clipboard.copy(editSession);
			SchematicFormat.MCEDIT.save(clipboard, saveFile);
			editSession.flushQueue();
		} catch (DataException e) {
			ChessUtils.errorMessage(player, Messages.getString("TerrainBackup.cantWriteTerrain", e.getMessage())); //$NON-NLS-1$
		} catch (IOException e) {
			ChessUtils.errorMessage(player, Messages.getString("TerrainBackup.cantWriteTerrain", e.getMessage())); //$NON-NLS-1$
		}
	}

	private boolean reloadTerrain() {
		if (wep == null) {
			return false;
		}

		try {
			editSession.enableQueue();
			localSession.setClipboard(SchematicFormat.MCEDIT.load(saveFile));
			Vector pos = localSession.getClipboard().getOrigin();
			localSession.getClipboard().place(editSession, pos, false);
			editSession.flushQueue();
			we.flushBlockBag(localPlayer, editSession);
			if (!saveFile.delete()) {
				LogUtils.warning(Messages.getString("TerrainBackup.cantDeleteTerrain", saveFile)); //$NON-NLS-1$
			}
			return true;
		} catch (Exception e) {
			// DataException, IOException, EmptyClipboardException,
			// MaxChangedBlocksException
			ChessUtils.errorMessage(player, Messages.getString("TerrainBackup.cantRestoreTerrain", e.getMessage())); //$NON-NLS-1$
			return false;
		}
	}

	public static void save(Player player, BoardView view) {
		try {
			TerrainBackup tb = new TerrainBackup(player, view);
			tb.saveTerrain();
		} catch (FilenameException e) {
			LogUtils.warning(e.getMessage());
		}
	}

	public static boolean reload(Player player, BoardView view) {
		boolean restored = false;
		try {
			TerrainBackup tb = new TerrainBackup(player, view);
			restored = tb.reloadTerrain();
		} catch (FilenameException e) {
			LogUtils.warning(e.getMessage());
		}
		return restored;
	}
}
