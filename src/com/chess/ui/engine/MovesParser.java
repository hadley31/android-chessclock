package com.chess.ui.engine;

import android.text.TextUtils;
import com.chess.statics.AppConstants;
import com.chess.statics.Symbol;

import java.util.List;
import java.util.regex.Pattern;

public class MovesParser {
	/*
	Special Symbols
	x: captures
	0-0: kingside castle
	0-0-0: queenside castle
	+: check
	#: checkmate
	!: good move
	?: poor move
	more !s and ?s can be added for emphasis.
	*/

	// white pieces
	public static final String WHITE_QUEEN = "Q";
	public static final String WHITE_ROOK = "R";
	public static final String WHITE_BISHOP = "B";
	public static final String WHITE_KNIGHT = "N";
	public static final String WHITE_KING = "K";
	public static final String WHITE_PAWN = "P";
	// black pieces
	public static final String BLACK_QUEEN = "q";
	public static final String BLACK_ROOK = "r";
	public static final String BLACK_BISHOP = "b";
	public static final String BLACK_KNIGHT = "n";
	public static final String BLACK_KING = "k";
	public static final String BLACK_PAWN = "p";


	public static final char fileLetters[] = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
	public static final int rankNumbers[] = new int[]{1, 2, 3, 4, 5, 6, 7, 8};
	public static final String CAPTURE_MARK = "x";

	public static final String REGEXP_CHARS = "[a,b,c,d,e,f,g,h]";
	public static final String REGEXP_NUMBERS = "[0-9]";


	public static final String B_KINGSIDE_MOVE_CASTLING = "kg8";
	public static final String B_QUEENSIDE_MOVE_CASTLING = "kc8";
	public static final String KINGSIDE_CASTLING = "O-O";
	public static final String KINGSIDE_CASTLING_AND_CHECK = "O-O+";
	public static final String QUEENSIDE_CASTLING = "O-O-O";
	public static final String QUEENSIDE_CASTLING_AND_CHECK = "O-O-O+";

	private static final int PAWN = 0;
	private static final int KNIGHT = 1;
	private static final int BISHOP = 2;
	private static final int ROOK = 3;
	private static final int QUEEN = 4;
	private static final int KING = 5;

	public MovesParser() {	}

	String removeNumbers(String moves) {
		return moves.replaceAll(AppConstants.MOVE_NUMBERS_PATTERN, Symbol.EMPTY)
				.replaceAll("[.]", Symbol.EMPTY);
	}



	int[] parseCoordinate(ChessBoard board, String move) {
		List<Move> validMoves = board.generateLegalMoves();

		int promotion = 0;

		String[] moveTo = new String[2];
		String currentMove = move.trim();

		if (currentMove.equals(KINGSIDE_CASTLING) || currentMove.equals(KINGSIDE_CASTLING_AND_CHECK)) {
			if (board.getSide() == 0) {
				return new int[]{board.whiteKing, board.whiteKingMoveOO[0], 0, 2};
			} else if (board.getSide() == 1) {
				return new int[]{board.blackKing, board.blackKingMoveOO[0], 0, 2};
			}
		}
		if (currentMove.equals(QUEENSIDE_CASTLING) || currentMove.equals(QUEENSIDE_CASTLING_AND_CHECK)) {
			if (board.getSide() == 0) {
				return new int[]{board.whiteKing, board.whiteKingMoveOOO[0], 0, 2};
			} else if (board.getSide() == 1) {
				return new int[]{board.blackKing, board.blackKingMoveOOO[0], 0, 2};
			}
		}

		int end = currentMove.length() - 1;
		while (end != 0) {
			if (Pattern.matches(REGEXP_NUMBERS, currentMove.substring(end, end + 1))) {
				moveTo[0] = currentMove.substring(end - 1, end);
				moveTo[1] = currentMove.substring(end, end + 1);
				break;
			}
			end--;
		}

		int i = letterToBN(moveTo[0]);
		int j = numToBN(moveTo[1]);
		int to = j * 8 - i;
		int from = numToBN(Symbol.EMPTY + currentMove.charAt(1)) * 8 - letterToBN(Symbol.EMPTY + currentMove.charAt(0));

		for (Move validMove : validMoves) {
			if (validMove.from == from && validMove.to == to) {
				char lastChar = currentMove.charAt(currentMove.length() - 1);
				if (lastChar == 'q') {
					promotion = 4;
				} else if (lastChar == 'r') {
					promotion = 3;
				} else if (lastChar == 'b') {
					promotion = 2;
				} else if (lastChar == 'n') {
					promotion = 1;
				}
				return new int[]{from, to, promotion, validMove.bits};
			}
		}

		return new int[]{from, to, promotion};
	}

	/**
	 * Method will parse {@code String} move and return array of {@code int}
	 * <SAN move descriptor piece moves>   ::= <Piece symbol>[<from file>|<from rank>|<from square>]['x']<to square>
	 * <SAN move descriptor pawn captures> ::= <from file>[<from rank>] 'x' <to square>[<promoted to>]
	 * <SAN move descriptor pawn push>     ::= <to square>[<promoted to>]
	 * <p></p>
	 * {@code currentMove.substring(1, 2)} is a selection to identify Disambiguating move
	 * When two (or more) identical pieces can move to the same square, the moving piece is uniquely identified
	 * by specifying the piece's letter, followed by (in descending order of preference):
	 * the file of departure (if they differ); or
	 * the rank of departure (if the files are the same but the ranks differ); or
	 * both the file and rank (if neither alone is sufficient to identify the piece—which occurs only in rare cases
	 * where one or more pawns have promoted, resulting in a player having three or more identical pieces able to
	 * reach the same square).
	 * For example, with knights on g1 and d2, either of which might move to f3, the move is specified as Ngf3 or Ndf3,
	 * as appropriate. With knights on g5 and g1, the moves are N5f3 or N1f3. As above, an "x" can be inserted
	 * to indicate a capture, for example: N5xf3. Another example: two rooks on d3 and h5, either one of which
	 * may move to d5. If the rook on d3 moves to d5, it is possible to disambiguate with either Rdd5 or R3d5,
	 * but the file takes precedence over the rank, so Rdd5 is correct.
	 * (And likewise if the move is a capture, Rdxd5 is correct.)
	 * @param board to be used as resource to identify move from positions
	 * @param move String move representation like <Piece symbol>[<from file>|<from rank>|<from square>]['x']<to square>
	 * @return the {@code int} array of move(s)
	 * @see <a href="http://en.wikipedia.org/wiki/Algebraic_notation_(chess)>http://en.wikipedia.org/wiki/Algebraic_notation_(chess)</a>
	 */
	int[] parse(ChessBoard board, String move) {
		// possible values ,e4, e4xd5, Rfg1, Rdxd5
		move = removeNumbers(move);
		List<Move> validMoves = board.generateLegalMoves();

		int promotion = 0;

		String[] moveTo = new String[2];
		String currentMove = move.trim();

		if (currentMove.equals(KINGSIDE_CASTLING) || currentMove.equals(KINGSIDE_CASTLING_AND_CHECK)) {
			if (board.getSide() == ChessBoard.WHITE_SIDE) {
				return new int[]{board.whiteKing, board.whiteKingMoveOO[ChessBoard.BLACK_KINGSIDE_CASTLE], 0, 2};
			} else if (board.getSide() == ChessBoard.BLACK_SIDE) {
				return new int[]{board.blackKing, board.blackKingMoveOO[ChessBoard.BLACK_KINGSIDE_CASTLE], 0, 2};
			}
		}
		if (currentMove.equals(QUEENSIDE_CASTLING) || currentMove.equals(QUEENSIDE_CASTLING_AND_CHECK)) {
			if (board.getSide() == ChessBoard.WHITE_SIDE) {
				return new int[]{board.whiteKing, board.whiteKingMoveOOO[ChessBoard.BLACK_KINGSIDE_CASTLE], 0, 2};
			} else if (board.getSide() == ChessBoard.BLACK_SIDE) {
				return new int[]{board.blackKing, board.blackKingMoveOOO[ChessBoard.BLACK_KINGSIDE_CASTLE], 0, 2};
			}
		}

		// detecting destination square(file and rank)
		int end = currentMove.length() - 1;
		while (end != 0) {
			if (Pattern.matches(REGEXP_NUMBERS, currentMove.substring(end, end + 1))) { // if last symbol in string match number of square
				moveTo[0] = currentMove.substring(end - 1, end); // get letter coordinate
				moveTo[1] = currentMove.substring(end, end + 1); // get number coordinate
				break;
			}
			end--;
		}

		int i = letterToBN(moveTo[0]);
		int j = numToBN(moveTo[1]);
		int from = 0;
		int to = j * 8 - i;

		int pieceType = PAWN;
		String movePieceStr = currentMove.substring(0, 1);
		if (movePieceStr.contains(WHITE_KNIGHT)) {
			pieceType = KNIGHT;
		} else if (movePieceStr.contains(WHITE_BISHOP)) {
			pieceType = BISHOP;
		} else if (movePieceStr.contains(WHITE_ROOK)) {
			pieceType = ROOK;
		} else if (movePieceStr.contains(WHITE_QUEEN)) {
			pieceType = QUEEN;
		} else if (movePieceStr.contains(WHITE_KING)) {
			pieceType = KING;
		}

		// Rfg1 for example
		// check if we have Disambiguating move
		String fromSquare = Symbol.EMPTY; // can be any file or rank, or even square , e4xd5, Rfg1, Rdd5, Rdxd5, but the file takes precedence over the rank, so Rdd5 is correct.
		String fromFile = Symbol.EMPTY;
		String fromRank = Symbol.EMPTY;
		if (currentMove.length() > 3) { // i.e. 4  // if we have Piece, or capture mark, or from file/rank/square
			fromSquare = fromSquare.replaceAll("[+,!,?,#,x]", Symbol.EMPTY); // remove special symbols
			fromSquare = fromSquare.replaceAll("[N,R,K,Q,B]", Symbol.EMPTY); // remove Piece
			String destinationSquare = moveTo[0] + moveTo[1];
			fromSquare = fromSquare.replace(destinationSquare, Symbol.EMPTY); // remove destination square

			if (fromSquare.length() < 2) {
				if (fromSquare.matches(REGEXP_CHARS)) {
					fromFile = fromSquare;
				} else {
					fromRank = fromSquare;
				}
			}
		}

		// use Disambiguating move, and fill from data
		// if KNIGHT, BISHOP, ROOK OR QUEEN
		if ((pieceType >= KNIGHT && pieceType <= QUEEN)
				&& !fromSquare.contains(CAPTURE_MARK) // looks like always will be true
				&& !currentMove.substring(2, 3).matches(REGEXP_NUMBERS) // it check  ??? TODO investigate what is it for
				) {
			for (int k = 0; k < 64; k++) {
				if (!TextUtils.isEmpty(fromFile)) {
					int fromFileInt = (ChessBoard.getRow(k) + 1) * 8 - letterToBN(fromFile);

//					if (fromSquare.matches(REGEXP_CHARS)) {
						if (board.pieces[fromFileInt] == pieceType && board.colors[fromFileInt] == board.getSide()) { // if we have found that piece and color on board
							return new int[]{fromFileInt, to, promotion};
						}
//					}
				}
				if (!TextUtils.isEmpty(fromRank)) {
					int fromRankInt = numToBN(fromRank) * 8 - (ChessBoard.getColumn(k) + 1);
//					if (fromSquare.matches(REGEXP_NUMBERS)) {
						if (board.pieces[fromRankInt] == pieceType && board.colors[fromRankInt] == board.getSide()) { // if we have found that piece and color on board
							return new int[]{fromRankInt, to, promotion};
						}
//					}
				}
			}
		}

		// detect piece that was moved
		for (int pieceFrom = 0; pieceFrom < 64; pieceFrom++) {
			if (board.pieces[pieceFrom] == pieceType && board.colors[pieceFrom] == board.getSide()) {
				for (Move validMove : validMoves) {
					if (validMove.from == pieceFrom && validMove.to == to) {
						if (pieceType == BISHOP) { // TODO investigate why we have only BISHOP check here???
							if (board.getBoardColor()[pieceFrom] == board.getBoardColor()[to]) {
								return new int[]{pieceFrom, to, promotion};
							}
						} else if (pieceType == PAWN) {
							if (currentMove.contains(CAPTURE_MARK)
									&& 9 - letterToBN(movePieceStr) != ChessBoard.getColumn(pieceFrom) + 1) { // TODO investigate why do we break here?
								break;
							}

							if (currentMove.contains(WHITE_QUEEN)) {
								promotion = QUEEN;
							} else if (currentMove.contains(WHITE_ROOK)) {
								promotion = ROOK;
							} else if (currentMove.contains(WHITE_BISHOP)) {
								promotion = BISHOP;
							} else if (currentMove.contains(WHITE_KNIGHT)) {
								promotion = KNIGHT;
							}

							return new int[]{pieceFrom, to, promotion, validMove.bits};
						} else {
							return new int[]{pieceFrom, to, promotion};
						}
					}
				}
			}
		}

		return new int[]{from, to, promotion};
	}

	int letterToBN(String letter) {
		for (int i = 0; i < fileLetters.length; i++) {
			char symbol = fileLetters[i];
			if (letter.equalsIgnoreCase(String.valueOf(symbol))) {
				return 8 - i;
			}
		}
		throw new IllegalStateException(" letterToBN haven't found a needed int for symbol " + letter);

//		int number = 0;
//		String symbol = letter.toLowerCase();
//		if (symbol.contains(A_SMALL)) {
//			number = 8;
//		} else if (symbol.contains(B_SMALL)) {
//			number = 7;
//		} else if (symbol.contains(C_SMALL)) {
//			number = 6;
//		} else if (symbol.contains(D_SMALL)) {
//			number = 5;
//		} else if (symbol.contains(E_SMALL)) {
//			number = 4;
//		} else if (symbol.contains(F_SMALL)) {
//			number = 3;
//		} else if (symbol.contains(G_SMALL)) {
//			number = 2;
//		} else if (symbol.contains(H_SMALL)) {
//			number = 1;
//		}
//
//		return number;
	}

	int numToBN(String symbol) {
		return 9 - Integer.parseInt(symbol);
//		int j = 0;
//		if (symbol.contains(NUMB_1)) j = 8;
//		if (symbol.contains(NUMB_2)) j = 7;
//		if (symbol.contains(NUMB_3)) j = 6;
//		if (symbol.contains(NUMB_4)) j = 5;
//		if (symbol.contains(NUMB_5)) j = 4;
//		if (symbol.contains(NUMB_6)) j = 3;
//		if (symbol.contains(NUMB_7)) j = 2;
//		if (symbol.contains(NUMB_8)) j = 1;
//		return j;
	}

	String BNToLetter(int symbol) {
		return String.valueOf(fileLetters[symbol]);
//		String number = Symbol.EMPTY;
//		if (symbol == 7) {
//			number = H_SMALL;
//		} else if (symbol == 6) {
//			number = G_SMALL;
//		} else if (symbol == 5) {
//			number = F_SMALL;
//		} else if (symbol == 4) {
//			number = E_SMALL;
//		} else if (symbol == 3) {
//			number = D_SMALL;
//		} else if (symbol == 2) {
//			number = C_SMALL;
//		} else if (symbol == 1) {
//			number = B_SMALL;
//		} else if (symbol == 0) {
//			number = A_SMALL;
//		}
//
//		return number;
	}

	String BNToNum(int number) {
		return String.valueOf(8 - number);
//		String symbol = Symbol.EMPTY;
//		if (number == 7) {
//			symbol = NUMB_1;
//		} else if (number == 6) {
//			symbol = NUMB_2;
//		} else if (number == 5) {
//			symbol = NUMB_3;
//		} else if (number == 4) {
//			symbol = NUMB_4;
//		} else if (number == 3) {
//			symbol = NUMB_5;
//		} else if (number == 2) {
//			symbol = NUMB_6;
//		} else if (number == 1) {
//			symbol = NUMB_7;
//		} else if (number == 0) {
//			symbol = NUMB_8;
//		}
//
//		return symbol;
	}

	String positionToString(int pos) {
		return ChessBoard.Board.values()[pos].toString().toLowerCase();
	}


}