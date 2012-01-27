/**
 * Programmer: Jacob Scott
 * Program Name: BoardStyle
 * Description: for wrapping up all board settings
 * Date: Jul 29, 2011
 */
package me.desht.chesscraft.chess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import me.desht.chesscraft.ChessConfig;
import me.desht.chesscraft.blocks.BlockType;
import me.desht.chesscraft.blocks.MaterialWithData;
import me.desht.chesscraft.enums.HighlightStyle;
import me.desht.chesscraft.exceptions.ChessException;
import org.yaml.snakeyaml.Yaml;

public class BoardStyle {

	public static final int MIN_HEIGHT = 3, MIN_FRAMEWIDTH = 2, MIN_SQUARESIZE = 1;
	public static final int MAX_HEIGHT = 128, MAX_FRAMEWIDTH = 20, MAX_SQUARESIZE = 20;
	
	int frameWidth, squareSize, height;
	MaterialWithData blackSquareMat, whiteSquareMat;
	MaterialWithData enclosureMat, frameMat, controlPanelMat;
	MaterialWithData highlightMat, highlightWhiteSquareMat, highlightBlackSquareMat;
	HighlightStyle highlightStyle;
	int lightLevel;
	String styleName;
	String pieceStyleName;

	// protected - have to use BoardStyle.loadNewStyle to get a new one..
	protected BoardStyle() {
	}

	public String getName() {
		return styleName;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getHeight() {
		return height;
	}

	public int getSquareSize() {
		return squareSize;
	}

	public int getLightLevel() {
		return lightLevel;
	}

	public void setLightLevel(int lightLevel) {
		this.lightLevel = lightLevel;
	}

	public MaterialWithData getBlackSquareMaterial() {
		return blackSquareMat;
	}

	public MaterialWithData getWhiteSquareMaterial() {
		return whiteSquareMat;
	}

	public MaterialWithData getControlPanelMaterial() {
		return controlPanelMat == null ? frameMat : controlPanelMat;
	}

	public MaterialWithData getEnclosureMaterial() {
		return enclosureMat;
	}

	public MaterialWithData getFrameMaterial() {
		return frameMat;
	}

	public HighlightStyle getHighlightStyle() {
		return highlightStyle;
	}

	public MaterialWithData getHighlightMaterial() {
		return highlightMat;
	}

	public MaterialWithData getHighlightMaterial(boolean isWhiteSquare) {
		return isWhiteSquare ? getWhiteSquareHighlightMaterial() : getBlackSquareHighlightMaterial();
	}

	public MaterialWithData getBlackSquareHighlightMaterial() {
		return highlightBlackSquareMat == null ? highlightMat : highlightBlackSquareMat;
	}

	public MaterialWithData getWhiteSquareHighlightMaterial() {
		return highlightWhiteSquareMat == null ? highlightMat : highlightWhiteSquareMat;
	}

	public void setBlackSquareMaterial(MaterialWithData blackSquareMat) {
		if (blackSquareMat != null) {
			this.blackSquareMat = blackSquareMat;
		}
	}

	public void setWhiteSquareMat(MaterialWithData whiteSquareMat) {
		if (whiteSquareMat != null) {
			this.whiteSquareMat = whiteSquareMat;
		}
	}

	public void setControlPanelMaterial(MaterialWithData controlPanelMat) {
		if (controlPanelMat != null && !BlockType.canPassThrough(controlPanelMat.getMaterial())) {
			this.controlPanelMat = controlPanelMat;
		}
	}

	public void setEnclosureMaterial(MaterialWithData enclosureMat) {
		if (enclosureMat == null) {
			this.enclosureMat = new MaterialWithData(0);
		} else {
			this.enclosureMat = enclosureMat;
		}
	}

	public void setFrameMaterial(MaterialWithData frameMat) {
		if (frameMat != null) {
			this.frameMat = frameMat;
		}
	}

	private void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth < MIN_FRAMEWIDTH ? MIN_FRAMEWIDTH
				: (frameWidth > MAX_FRAMEWIDTH ? MAX_FRAMEWIDTH : frameWidth);
	}

	private void setHeight(int height) {
		this.height = height < MIN_HEIGHT ? MIN_HEIGHT
				: (height > MAX_HEIGHT ? MAX_HEIGHT : height);
	}

	public void setHighlightBlackSquareMaterial(MaterialWithData highlightBlackSquareMat) {
		this.highlightBlackSquareMat = highlightBlackSquareMat;
	}

	public void setHighlightMaterial(MaterialWithData highlightMat) {
		this.highlightMat = highlightMat;
	}

	public void setHighlightStyle(HighlightStyle highlightStyle) {
		this.highlightStyle = highlightStyle;
	}

	public void setHighlightWhiteSquareMaterial(MaterialWithData highlightWhiteSquareMat) {
		this.highlightWhiteSquareMat = highlightWhiteSquareMat;
	}

	private void setSquareSize(int squareSize) {
		this.squareSize = squareSize < MIN_SQUARESIZE ? MIN_SQUARESIZE
				: (squareSize > MAX_SQUARESIZE ? MAX_SQUARESIZE : squareSize);
	}

	public void saveStyle(String newStyleName) throws ChessException {
		File newFile = new File(ChessConfig.getBoardStyleDirectory(), newStyleName + ".yml");
		
		// yeah, it would be nice to use YAML libs to save this, but I'm putting comments in
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(newFile));
			out.write("# Chess board style definition\n\n");
			out.write("# NOTE: all materials must be quoted, even if they're just integers, or\n");
			out.write("# you will get a java.lang.ClassCastException when the style is loaded.\n\n");
			out.write("# width/length of the board squares, in blocks\n");
			out.write("square_size: " + squareSize + "\n");
			out.write("# width in blocks of the frame surrounding the board\n");
			out.write("frame_width: " + frameWidth + "\n");
			out.write("# height of the board - number of squares of clear air between board and enclosure roof\n");
			out.write("height: " + height + "\n");
			out.write("# material/data for the white squares\n");
			out.write("white_square: '" + whiteSquareMat + "'\n");
			out.write("# material/data for the black squares\n");
			out.write("black_square: '" + blackSquareMat + "'\n");
			out.write("# material/data for the frame\n");
			out.write("frame: '" + frameMat + "'\n");
			out.write("# material/data for the enclosure (if you don't use glass or air, then light the board!)\n");
			out.write("enclosure: '" + enclosureMat + "'\n");
			out.write("# board lighting level (0-15)\n");
			out.write("light_level: " + lightLevel + "\n");
			out.write("# style of chess set to use (see ../pieces/*.yml)\n");
			out.write("# the style chosen must fit within the square_size specified above\n");
			out.write("piece_style: " + pieceStyleName + "\n");
			out.write("# material/data for the control panel (default: 'frame' setting)\n");
			out.write("panel: " + controlPanelMat + "\n");
			out.write("# highlighting style (NONE, CORNERS, EDGES, LINE, CHECKERED)\n");
			out.write("highlight_style: " + highlightStyle + "\n");
			out.write("# highlighting material (default: glowstone)\n");
			out.write("highlight: " + highlightMat + "\n");
			out.write("# highlighting material on white squares (default: 'highlight' setting)\n");
			out.write("highlight: " + highlightWhiteSquareMat + "\n");
			out.write("# highlighting material on black squares (default: 'highlight' setting)\n");
			out.write("highlight: " + highlightBlackSquareMat + "\n");
			out.close();
			
			styleName = newStyleName;
		} catch (IOException e) {
			throw new ChessException(e.getMessage());
		}
	}

	public static BoardStyle loadNewStyle(String boardStyle) throws FileNotFoundException, ChessException {
		Yaml yaml = new Yaml();
		
		File f = new File(ChessConfig.getBoardStyleDirectory(), boardStyle + ".yml");

		FileInputStream in = new FileInputStream(f);
		@SuppressWarnings("unchecked")
		Map<String, Object> styleMap = (Map<String, Object>) yaml.load(in);

		for (String k : new String[]{"square_size", "frame_width", "height",
					"black_square", "white_square", "frame", "enclosure"}) {
			if (!styleMap.containsKey(k)) {
				throw new ChessException("required field '" + k + "' is missing");
			}
		}
		if (!styleMap.containsKey("lit") && !styleMap.containsKey("light_level")) {
			throw new ChessException("must have at least one of 'lit' or 'light_level'");
		}
		
		BoardStyle style = new BoardStyle();
		style.styleName = f.getName().replaceFirst("\\.yml$", "");

		style.setSquareSize((Integer) styleMap.get("square_size"));
		style.setFrameWidth((Integer) styleMap.get("frame_width"));
		style.setHeight((Integer) styleMap.get("height"));
		style.pieceStyleName = (String) styleMap.get("piece_style");

		if (styleMap.containsKey("lit")) {
			style.lightLevel = 15;
		} else {
			style.lightLevel = (Integer) styleMap.get("light_level");
		}

		style.blackSquareMat = new MaterialWithData((String) styleMap.get("black_square"));
		style.whiteSquareMat = new MaterialWithData((String) styleMap.get("white_square"));
		style.frameMat = new MaterialWithData((String) styleMap.get("frame"));
		style.enclosureMat = new MaterialWithData((String) styleMap.get("enclosure"));

		/**************  added optional parameters  **************/
		if (styleMap.get("panel") != null) {
			style.controlPanelMat = new MaterialWithData((String) styleMap.get("panel"));
		}
		if (styleMap.get("highlight") != null) {
			style.highlightMat = new MaterialWithData((String) styleMap.get("highlight"));
		} else {
			style.highlightMat = new MaterialWithData(89);
		}

		if (styleMap.get("highlight_white_square") != null) {
			style.highlightWhiteSquareMat = new MaterialWithData(
					(String) styleMap.get("highlight_white_square"));
		} else {
			style.highlightWhiteSquareMat = null;
		}

		if (styleMap.get("highlight_black_square") != null) {
			style.highlightBlackSquareMat = new MaterialWithData(
					(String) styleMap.get("highlight_black_square"));
		} else {
			style.highlightBlackSquareMat = null;
		}

		style.highlightStyle = HighlightStyle.getStyle((String) styleMap.get("highlight_style"));
		if (style.highlightStyle == null) {
			style.highlightStyle = HighlightStyle.CORNERS;
		}

		return style;
	}
}
