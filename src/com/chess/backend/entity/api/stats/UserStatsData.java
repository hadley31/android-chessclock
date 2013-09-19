package com.chess.backend.entity.api.stats;

import com.chess.statics.Symbol;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 04.02.13
 * Time: 13:24
 */
public class UserStatsData {
/*
	"rating": "Unrated",
	"highest_rating": 1200,
	"avg_oponent_rating": 0,
	"total_games": 6,
	"wins": 1,
	"losses": 5,
	"draws": 0,
	"best_win_rating": null,
	"best_win_username": null
*/

	private int rating;
	private int highest_rating;
	private float avg_oponent_rating;
	private int total_games;
	private int wins;
	private int losses;
	private int draws;
	private int best_win_rating;
	private String best_win_username;


	public int getRating() {
		return rating;
	}

	public int getHighestRating() {
		return highest_rating;
	}

	public float getAvgOpponentRating() {
		return avg_oponent_rating;
	}

	public int getTotalGames() {
		return total_games;
	}

	public int getWins() {
		return wins;
	}

	public int getLosses() {
		return losses;
	}

	public int getDraws() {
		return draws;
	}

	public int getBestWinRating() {
		return best_win_rating;
	}

	public String getBestWinUsername() {
		return best_win_username == null ? Symbol.EMPTY : best_win_username;
	}
}
