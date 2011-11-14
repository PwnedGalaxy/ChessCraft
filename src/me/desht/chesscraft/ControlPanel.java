package me.desht.chesscraft;

import me.desht.chesscraft.chess.BoardView;
import me.desht.chesscraft.chess.ChessGame;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import chesspresso.Chess;

import me.desht.chesscraft.enums.GameState;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.exceptions.ChessException;
import me.desht.chesscraft.enums.Direction;
import me.desht.chesscraft.enums.ExpectAction;
import me.desht.chesscraft.regions.Cuboid;
import me.desht.chesscraft.util.ChessUtils;
import me.desht.chesscraft.util.PermissionUtils;
import me.desht.chesscraft.blocks.SignButton;
import me.desht.chesscraft.enums.BoardOrientation;
import me.desht.chesscraft.log.ChessCraftLogger;

public class ControlPanel {

	// Button names.  These should correspond directly to the equivalent /chess subcommand name
	// with spaces replaced by dots.  If there's no corresponding command, prefix the name with a
	// "*" (this ensures a permission check isn't done).
	private static final String STAKE = "stake";
	private static final String BLACK_NO = "*black-no";
	private static final String WHITE_NO = "*white-no";
	private static final String BLACK_YES = "*black-yes";
	private static final String WHITE_YES = "*white-yes";
	private static final String BLACK_PROMOTE = "*black-promote";
	private static final String WHITE_PROMOTE = "*white-promote";
	private static final String TELEPORT = "teleport";
	private static final String INVITE_ANYONE = "invite.anyone";
	private static final String INVITE_PLAYER = "invite";
	private static final String BOARD_INFO = "list.board";
	private static final String GAME_INFO = "list.game";
	private static final String OFFER_DRAW = "offer.draw";
	private static final String RESIGN = "resign";
	private static final String START = "start";
	private static final String CREATE_GAME = "create.game";
	
	public static final int PANEL_WIDTH = 8;
	private BoardView view;
	private BoardOrientation boardDir = null, signDir = null;
	private MaterialWithData signMat;
	private Cuboid panelBlocks;
	private Cuboid toMoveIndicator;
	private Location halfMoveClockSign;
	private Location whiteClockSign;
	private Location blackClockSign;
	private Location plyCountSign;
	private Map<String, SignButton> buttons;
	private Map<Location, SignButton> buttonLocs;

	public ControlPanel(BoardView view) {
		this.view = view;
		boardDir = view.getDirection();
		signDir = boardDir.getRight();

		buttons = new HashMap<String, SignButton>();
		buttonLocs = new HashMap<Location, SignButton>();

		panelBlocks = getBoardControlPanel(view);

		toMoveIndicator = panelBlocks.clone();
		toMoveIndicator.inset(Direction.Vertical, 1).
				expand(boardDir.getDirection(), -((PANEL_WIDTH - 2) / 2)).
				expand(boardDir.getDirection().opposite(), -((PANEL_WIDTH - 2) / 2));
		// .inset(Direction.Horizontal, PANEL_WIDTH / 2);
		//if (view.getName().contains("ter")) toMoveIndicator.weSelect(plugin.getServer().getPlayer("jascotty2"));

		signMat = new MaterialWithData(68, getSignDir(signDir));

		halfMoveClockSign = getSignLocation(2, 0);
		plyCountSign = getSignLocation(5, 0);
		whiteClockSign = getSignLocation(2, 1);
		blackClockSign = getSignLocation(5, 1);
	}

	public void repaint() {
		World w = view.getA1Square().getWorld();
		for (Location l : panelBlocks) {
			view.getControlPanelMat().applyToBlock(w.getBlockAt(l));
		}

		ChessGame game = view.getGame();
		view.toPlayChanged(game != null ? game.getPosition().getToPlay() : Chess.NOBODY);

		signMat.applyToBlock(halfMoveClockSign.getBlock());
		updateHalfMoveClock(game == null ? 0 : game.getPosition().getHalfMoveClock());

		signMat.applyToBlock(plyCountSign.getBlock());
		updatePlyCount(game == null ? 0 : game.getPosition().getPlyNumber());

		signMat.applyToBlock(whiteClockSign.getBlock());
		signMat.applyToBlock(blackClockSign.getBlock());

		updateClock(Chess.WHITE, game == null ? 0 : game.getTimeWhite());
		updateClock(Chess.BLACK, game == null ? 0 : game.getTimeBlack());

		repaintSignButtons();
	}

	public void repaintSignButtons() {
		ChessGame game = view.getGame();

		boolean settingUp = game != null && game.getState() == GameState.SETTING_UP;
		boolean running = game != null && game.getState() == GameState.RUNNING;
		boolean hasWhite = game != null && !game.getPlayerWhite().isEmpty();
		boolean hasBlack = game != null && !game.getPlayerBlack().isEmpty();

		createSignButton(0, 2, BOARD_INFO, Messages.getString("ControlPanel.boardInfoBtn"), signMat, true); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(0, 1, TELEPORT, Messages.getString("ControlPanel.teleportOutBtn"), signMat, true); //$NON-NLS-1$ //$NON-NLS-2$
		if (ChessCraft.economy != null) {
			createSignButton(7, 1, STAKE, getStakeStr(game), signMat, game != null); //$NON-NLS-1$
		}

		createSignButton(1, 2, CREATE_GAME, Messages.getString("ControlPanel.createGameBtn"), signMat, game == null); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(2, 2, INVITE_PLAYER, Messages.getString("ControlPanel.invitePlayerBtn"), signMat, settingUp //$NON-NLS-1$ //$NON-NLS-2$
				&& (!hasWhite || !hasBlack));
		createSignButton(3, 2, INVITE_ANYONE, Messages.getString("ControlPanel.inviteAnyoneBtn"), signMat, settingUp //$NON-NLS-1$ //$NON-NLS-2$
				&& (!hasWhite || !hasBlack));
		createSignButton(4, 2, START, Messages.getString("ControlPanel.startGameBtn"), signMat, settingUp); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(5, 2, OFFER_DRAW, Messages.getString("ControlPanel.offerDrawBtn"), signMat, running); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(6, 2, RESIGN, Messages.getString("ControlPanel.resignBtn"), signMat, running); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(7, 2, GAME_INFO, Messages.getString("ControlPanel.gameInfoBtn"), signMat, game != null); //$NON-NLS-1$ //$NON-NLS-2$

		createSignButton(1, 1, WHITE_PROMOTE, Messages.getString("ControlPanel.whitePawnPromotionBtn") + getPromoStr(game, Chess.WHITE), //$NON-NLS-1$ //$NON-NLS-2$
				signMat, hasWhite);
		createSignButton(6, 1, BLACK_PROMOTE, Messages.getString("ControlPanel.blackPawnPromotionBtn") + getPromoStr(game, Chess.BLACK), //$NON-NLS-1$ //$NON-NLS-2$
				signMat, hasBlack);

		Player pw = game == null ? null : Bukkit.getServer().getPlayer(game.getPlayerWhite());
		String offerw = getOfferText(pw);
		createSignButton(0, 0, WHITE_YES, offerw + Messages.getString("ControlPanel.yesBtn"), signMat, !offerw.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(1, 0, WHITE_NO, offerw + Messages.getString("ControlPanel.noBtn"), signMat, !offerw.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		Player pb = game == null ? null : Bukkit.getServer().getPlayer(game.getPlayerBlack());
		String offerb = getOfferText(pb);
		createSignButton(6, 0, BLACK_YES, offerb + ";;Yes", signMat, !offerb.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
		createSignButton(7, 0, BLACK_NO, offerb + ";;No", signMat, !offerb.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public Location getLocationTP(){
		Location l = (new Cuboid(toMoveIndicator.getCenter())).
				shift(signDir.getDirection(), 3).
				shift(Direction.Down, 1).getLowerNE();
		l.setYaw((signDir.getYaw() + 180.0f) % 360);
		return l;
	}

	private String getOfferText(Player p) {
		if (p == null) {
			return ""; //$NON-NLS-1$
		} else if (ChessCraft.expecter.isExpecting(p, ExpectAction.DrawResponse)) {
			return Messages.getString("ControlPanel.acceptDrawBtn"); //$NON-NLS-1$
		} else if (ChessCraft.expecter.isExpecting(p, ExpectAction.SwapResponse)) {
			return Messages.getString("ControlPanel.acceptSwapBtn"); //$NON-NLS-1$
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private Location getSignLocation(int x, int y) {
		int realX = signDir.getX(),
				realY = panelBlocks.getLowerNE().getBlockY() + y,
				realZ = signDir.getZ();

		switch(signDir){
			case NORTH:
				realX += panelBlocks.getLowerX();
				realZ += panelBlocks.getLowerZ() + x;
				break;
			case EAST:
				realX += panelBlocks.getUpperX() - x;
				realZ += panelBlocks.getLowerZ();
				break;
			case SOUTH:
				realX += panelBlocks.getLowerX();
				realZ += panelBlocks.getUpperZ() - x;
				break;
			case WEST:
				realX += panelBlocks.getLowerX() + x;
				realZ += panelBlocks.getLowerZ();
				break;
		}
		return new Location(view.getA1Square().getWorld(), realX, realY, realZ);
	}

	private void createSignButton(int x, int y, String name, String text, MaterialWithData m, boolean enabled) {
		SignButton button = getSignButton(name);

		if (button != null) {
			button.setText(text.replace("\n", ";"));
			button.setEnabled(enabled);
			button.repaint();
		} else {
			Location loc = getSignLocation(x, y);
			button = new SignButton(name, loc, text, m, enabled);
			button.repaint();
			buttons.put(name, button);
			buttonLocs.put(loc, button);
		}
	}

	public void updateSignButtonText(String name, String text) {
		SignButton button = getSignButton(name);
		if (button != null) {
			button.setText(text);
			button.repaint();
		}
	}

	public SignButton getSignButton(String name) {
		return buttons.get(name);
	}

	public Cuboid getPanelBlocks() {
		return panelBlocks;
	}

	public void signClicked(Player player, Block block, BoardView view, Action action) throws ChessException {
		ChessGame game = view.getGame();
		SignButton button = buttonLocs.get(block.getLocation());

		if (button == null) {
			return;
		}

		if (!button.isEnabled()) {
			return;
		}

		String name = button.getName();
		if (!name.startsWith("*")) {
			PermissionUtils.requirePerms(player, "chesscraft.commands." + name);
		}
		
		if (name.equals(CREATE_GAME)) { //$NON-NLS-1$
			ChessGame.createGame(player, null, view.getName());
		} else if (name.equals(START)) { //$NON-NLS-1$
			if (game != null) {
				game.start(player.getName());
			}
		} else if (name.equals(RESIGN)) { //$NON-NLS-1$
			if (game != null) {
				game.resign(player.getName());
			}
		} else if (name.equals(OFFER_DRAW)) { //$NON-NLS-1$
			if (game != null) {
				game.offerDraw(player);
			}
		} else if (name.equals(GAME_INFO)) { //$NON-NLS-1$
			if (game != null) {
				game.showGameDetail(player);
			}
		} else if (name.equals(BOARD_INFO)) { //$NON-NLS-1$
			view.showBoardDetail(player);
		} else if (name.equals(INVITE_PLAYER)) { //$NON-NLS-1$
			if (game != null && (game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty())) {
				ChessUtils.statusMessage(player, Messages.getString("ControlPanel.chessInviteReminder")); //$NON-NLS-1$
			}
		} else if (name.equals(INVITE_ANYONE)) { //$NON-NLS-1$
			if (game != null) {
				game.inviteOpen(player.getName());
			}
		} else if (name.equals(TELEPORT)) { //$NON-NLS-1$
			BoardView.teleportOut(player);
		} else if (name.equals(WHITE_PROMOTE)) { //$NON-NLS-1$
			game.cyclePromotionPiece(player.getName());
			view.getControlPanel().updateSignButtonText(WHITE_PROMOTE, "=;=;;&4" + getPromoStr(game, Chess.WHITE)); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (name.equals(BLACK_PROMOTE)) { //$NON-NLS-1$
			game.cyclePromotionPiece(player.getName());
			view.getControlPanel().updateSignButtonText(BLACK_PROMOTE, "=;=;;&4" + getPromoStr(game, Chess.BLACK)); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (name.equals(WHITE_YES) || name.equals(BLACK_YES)) { //$NON-NLS-1$ //$NON-NLS-2$
			ChessCraft.handleExpectedResponse(player, true);
		} else if (name.equals(WHITE_NO) || name.equals(BLACK_NO)) { //$NON-NLS-1$ //$NON-NLS-2$
			ChessCraft.handleExpectedResponse(player, true);
		} else if (name.equals(STAKE) && ChessCraft.economy != null) { //$NON-NLS-1$
			double stakeIncr;
			if (player.isSneaking()) {
				stakeIncr = ChessConfig.getConfig().getDouble("stake.smallIncrement"); //$NON-NLS-1$
			} else {
				stakeIncr = ChessConfig.getConfig().getDouble("stake.largeIncrement"); //$NON-NLS-1$
			}
			if (action == Action.RIGHT_CLICK_BLOCK) {
				stakeIncr = -stakeIncr;
			}
			if (game == null || (!game.getPlayerWhite().isEmpty() && !game.getPlayerBlack().isEmpty())) {
				return;
			}
			game.adjustStake(stakeIncr);
			view.getControlPanel().updateSignButtonText(STAKE, getStakeStr(game)); //$NON-NLS-1$
		}
	}

	private String getStakeStr(ChessGame game) {
		if (game == null) {
			double stake = ChessConfig.getConfig().getDouble("stake.default"); //$NON-NLS-1$
			String stakeStr = ChessCraft.economy.format(stake).replaceFirst(" ", ";"); //$NON-NLS-1$ //$NON-NLS-2$
			return Messages.getString("ControlPanel.stakeBtn") + stakeStr; //$NON-NLS-1$
		} else {
			double stake = game.getStake();
			String stakeStr = ChessCraft.economy.format(stake).replaceFirst(" ", ";&4"); //$NON-NLS-1$ //$NON-NLS-2$
			String col = game.getPlayerWhite().isEmpty() || game.getPlayerBlack().isEmpty() ? "&1" : "&0"; //$NON-NLS-1$ //$NON-NLS-2$
			return col + "Stake;;&4" + stakeStr; //$NON-NLS-1$
		}
	}

	private String getPromoStr(ChessGame game, int colour) {
		if (game == null) {
			return "?"; //$NON-NLS-1$
		}
		return ChessUtils.pieceToStr(game.getPromotionPiece(colour));
	}

	public void updateToMoveIndicator(MaterialWithData mat) {
		for (Location l : toMoveIndicator) {
			mat.applyToBlock(l.getBlock());
		}
	}

	public void updatePlyCount(int playNumber) {
		if (plyCountSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) plyCountSign.getBlock().getState();
			setSignLabel(s, Messages.getString("ControlPanel.playNumber")); //$NON-NLS-1$
			s.setLine(2, ChessUtils.parseColourSpec("&4" + playNumber)); //$NON-NLS-1$
			s.update();
		}

	}

	public void updateHalfMoveClock(int halfMoveClock) {
		if (halfMoveClockSign.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) halfMoveClockSign.getBlock().getState();
			setSignLabel(s, Messages.getString("ControlPanel.halfmoveClock")); //$NON-NLS-1$
			s.setLine(2, ChessUtils.parseColourSpec("&4" + halfMoveClock)); //$NON-NLS-1$
			s.update();
		}
	}

	public void updateClock(int colour, int t) {
		Location l;
		if (colour == Chess.WHITE) {
			l = whiteClockSign;
		} else {
			l = blackClockSign;
		}
		if (l.getBlock().getState() instanceof Sign) {
			Sign s = (Sign) l.getBlock().getState();
			setSignLabel(s, ChessGame.getColour(colour));
			s.setLine(2, ChessUtils.parseColourSpec("&4" + ChessGame.secondsToHMS(t))); //$NON-NLS-1$
			s.update();
		}
	}

	private void setSignLabel(Sign s, String text) {
		String[] lines = text.split(";");
		if (lines.length == 1) {
			s.setLine(0, "");
			s.setLine(1, lines[0]);
		} else if (lines.length == 2) {
			s.setLine(0, lines[0]);
			s.setLine(1, lines[1]);
		}
	}
	
	protected static Cuboid getBoardControlPanel(BoardView view) {

		BoardOrientation dir = view.getDirection();

		//Cuboid bounds = view.getBounds();
		Location a1 = view.getA1Square();

		int x = a1.getBlockX(), y = a1.getBlockY() + 1, z = a1.getBlockZ();

		// apply applicable rotation (panel on the left-side of board)
		switch (dir) {
			case NORTH:
				x -= (4 * view.getSquareSize() - PANEL_WIDTH / 2);
				z += (int) Math.ceil((view.getFrameWidth() + .5) / 2);
				break;
			case EAST:
				z -= (4 * view.getSquareSize() - PANEL_WIDTH / 2);
				x -= (int) Math.ceil((view.getFrameWidth() + .5) / 2);
				break;
			case SOUTH:
				x += (4 * view.getSquareSize() - PANEL_WIDTH / 2);
				z -= (int) Math.ceil((view.getFrameWidth() + .5) / 2);
				break;
			case WEST:
				z += (4 * view.getSquareSize() - PANEL_WIDTH / 2);
				x += (int) Math.ceil((view.getFrameWidth() + .5) / 2);
				break;
			default:
				ChessCraftLogger.severe("Unexpected BoardOrientation value ", new Exception());
				return null;
		}

		Cuboid panel = new Cuboid(new Location(a1.getWorld(), x, y, z));
		return panel.expand(dir.getDirection(), PANEL_WIDTH - 1).expand(Direction.Up, 2);

	}
	
	protected static byte getSignDir(BoardOrientation signDir){
		switch(signDir){
			case NORTH:
				return 4;
			case EAST:
				return 2;
			case SOUTH:
				return 5;
			case WEST:
				return 3;
			default:
				return 0;
		}
	}
}
