package com.chess.ui.engine;

final public class HistoryData {
	public Move m;
	public int capture;
	int ep;
	int fifty;
	boolean castleMask[] = {false, false, false, false};
	public int what = -1;
	public String notation;
	public boolean whiteCanCastle = true;
	public boolean blackCanCastle = true;
}
