package com.chess.lcc.android;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.StaticData;
import com.chess.live.client.*;
import com.chess.live.client.impl.HttpClientProvider;
import com.chess.live.util.GameTimeConfig;
import com.chess.live.util.config.Config;
import com.chess.model.GameItem;
import com.chess.model.GameListItem;
import com.chess.ui.activities.GameLiveScreenActivity;
import com.chess.ui.interfaces.LccConnectionListener;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LccHolder {
	final static Config CONFIG = new Config(StaticData.SYMBOL_EMPTY, "assets/my.properties", true);

	//static MemoryUsageMonitor muMonitor = new MemoryUsageMonitor(15);

	public static final String HOST = "chess.com";
	public static final String AUTH_URL = "http://www." + HOST + "/api/v2/login?username=%s&password=%s";
	public static final String CONFIG_BAYEUX_HOST = "live." + HOST;

	/*public static final String HOST = "10.0.2.2";
	  public static final String AUTH_URL = "http://" + HOST + "/api/v2/login?username=%s&password=%s";
	  public static final String CONFIG_BAYEUX_HOST = HOST;*/

	//Config.get(CONFIG.getString("live.chess.client.demo.chat_generator.connection.bayeux.host"), "live.chess-4.com");
	public static final Integer CONFIG_PORT = 80;
	public static final String CONFIG_URI =
			Config.get(CONFIG.getString("live.chess.client.demo.chat_generator.connection.bayeux.uri"), "/cometd");
	/*public static final String CONFIG_AUTH_KEY =
			Config.get(CONFIG.getString("live.chess.client.demo.chat_generator.connection.user1.authKey"),
					"FIXED_PHPSESSID_WEBTIDE_903210957432054387723");*/
	public static final String PKCS_12 = "PKCS12";
	public static final String TESTTEST = "testtest";
	public long previousFGTime;
	public long currentFGTime;
	public long currentFGGameId;
	public long previousFGGameId;

	private ChatListenerImpl _chatListener;
	private ConnectionListenerImpl _connectionListener;
	private LccGameListener _gameListener;
	private LiveChessClient _lccClient;
	private User _user;
	private static LccHolder instance;
	
	private Context context;
//	/**
//	 * Use android.util.Log instead
//	 */
//	@Deprecated
	public static final Logger LOG = Logger.getLogger(LccHolder.class);
	private AndroidStuff androidStuff;
	public static final int OWN_SEEKS_LIMIT = 3;


	private HashMap<Long, Challenge> challenges = new HashMap<Long, Challenge>();
	private final Hashtable<Long, Challenge> seeks = new Hashtable<Long, Challenge>();
	private HashMap<Long, Challenge> ownChallenges = new HashMap<Long, Challenge>();
	private Collection<? extends User> blockedUsers = new HashSet<User>();
	private Collection<? extends User> blockingUsers = new HashSet<User>();
	private final Hashtable<Long, Game> lccGames = new Hashtable<Long, Game>();
	private final Map<String, User> friends = new HashMap<String, User>();
	private final Map<String, User> onlineFriends = new HashMap<String, User>();
	private Map<GameEvent.Event, GameEvent> pausedActivityGameEvents = new HashMap<GameEvent.Event, GameEvent>();
	private final HashMap<Long, Chat> gameChats = new HashMap<Long, Chat>();
	private LinkedHashMap<Chat, LinkedHashMap<Long, ChatMessage>> receivedChatMessages =
			new LinkedHashMap<Chat, LinkedHashMap<Long, ChatMessage>>();

	private final LccChallengeListener challengeListener;
	private final LccSeekListListener seekListListener;
	private final LccFriendStatusListener friendStatusListener;
	private SubscriptionId seekListSubscriptionId;
	private boolean connected;
	private boolean nextOpponentMoveStillNotMade;
	private final Object opponentClockStartSync = new Object();
	private Timer opponentClockDelayTimer = new Timer("OpponentClockDelayTimer", true);
	private ChessClock whiteClock;
	private ChessClock blackClock;
	private boolean connectingInProgress;
	private boolean activityPausedMode = true;
	private Integer latestMoveNumber;
	private Long currentGameId;
	public String networkTypeName;
	private LccConnectionListener externalConnectionListener;

//	public LccHolder(InputStream keyStoreInputStream, String versionName) {
	public LccHolder(Context context) throws IOException, PackageManager.NameNotFoundException {
		this.context = context;
		androidStuff = new AndroidStuff(this);
		String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

		InputStream	keyStoreInputStream = context.getAssets().open("chesscom.pkcs12");
		Log.d("Chess.Com", "Start Chess.Com LCC mainApp");
		//System.setProperty("java.net.preferIPv6Addresses", "false");
		LOG.info("Connecting to: " + CONFIG_BAYEUX_HOST + ":" + CONFIG_PORT);
		//InputStream keyStoreInputStream = null;
		/*try
			{
			  keyStoreInputStream = new FileInputStream("/data/data/com.chess/chesscom.pkcs12");
			}
			catch(FileNotFoundException e)
			{
			  e.printStackTrace();
			}*/

		_lccClient = LiveChessClientFacade.createClient(AUTH_URL, CONFIG_BAYEUX_HOST, CONFIG_PORT, CONFIG_URI);
		_lccClient.setClientInfo("Android", versionName, "No-Key");
		_lccClient.setSupportedClientFeatures(false, false);
		//HttpClient httpClient = _lccClient.setHttpClientConfiguration(HttpClientProvider.DEFAULT_CONFIGURATION);
		HttpClient httpClient = HttpClientProvider.getHttpClient(HttpClientProvider.DEFAULT_CONFIGURATION, false);
		//httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
		httpClient.setConnectorType(HttpClient.CONNECTOR_SOCKET);
		httpClient.setMaxConnectionsPerAddress(4);
		httpClient.setSoTimeout(7000);
		httpClient.setConnectTimeout(10000);
		httpClient.setTimeout(7000); //

		httpClient.setKeyStoreType(PKCS_12);
		httpClient.setTrustStoreType(PKCS_12);
		httpClient.setKeyManagerPassword(TESTTEST);
		httpClient.setKeyStoreInputStream(keyStoreInputStream);
		httpClient.setKeyStorePassword(TESTTEST);
		httpClient.setTrustStoreInputStream(keyStoreInputStream);
		httpClient.setTrustStorePassword(TESTTEST);

		_lccClient.setHttpClient(httpClient);
		try {
			httpClient.start();
		} catch (Exception e) {
			throw new LiveChessClientException("Unable to initialize HttpClient", e);
		}

		_chatListener = new ChatListenerImpl(this);
		_connectionListener = new ConnectionListenerImpl(this);
		_gameListener = new LccGameListener(this);
		challengeListener = new LccChallengeListener(this);
		seekListListener = new LccSeekListListener(this);
		friendStatusListener = new LccFriendStatusListener(this);
	}

	public LccGameListener getGameListener() {
		return _gameListener;
	}

	public ChatListenerImpl getChatListener() {
		return _chatListener;
	}

	public ConnectionListenerImpl getConnectionListener() {
		return _connectionListener;
	}

	public User getUser() {
		return _user;
	}

	public void setUser(User user) {
		_user = user;
	}

	public LiveChessClient getClient() {
		return _lccClient;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public static LccHolder getInstance(Context context){
		if (instance == null) {
			try {
				instance = new LccHolder(context);
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	public AndroidStuff getAndroidStuff() {
		return androidStuff;
	}

	public void clearChallenges() {
		challenges.clear();
		androidStuff.updateChallengesList();
	}

//	public Challenge getChallenge(long challengeId) {
//		return challenges.get(challengeId);
//	}

	public void addOwnChallenge(Challenge challenge) {
		for (Challenge oldChallenge : ownChallenges.values()) {
			if (challenge.getGameTimeConfig().getBaseTime().equals(oldChallenge.getGameTimeConfig().getBaseTime())
					&& challenge.getGameTimeConfig().getTimeIncrement().equals(oldChallenge.getGameTimeConfig().getTimeIncrement())
					&& challenge.isRated() == oldChallenge.isRated()
					&& ((challenge.getTo() == null && oldChallenge.getTo() == null) ||
					(challenge.getTo() != null && challenge.getTo().equals(oldChallenge.getTo())))) {
				LOG.info("Check for doubled challenges: cancel challenge: " + oldChallenge);
				_lccClient.cancelChallenge(oldChallenge);
			}
		}
		ownChallenges.put(challenge.getId(), challenge);
	}

	public void storeBlockedUsers(Collection<? extends User> blockedUsers, Collection<? extends User> blockingUsers) {
		this.blockedUsers = blockedUsers;
		this.blockingUsers = blockingUsers;
	}

	public LccChallengeListener getChallengeListener() {
		return challengeListener;
	}

	public LccSeekListListener getSeekListListener() {
		return seekListListener;
	}

	public LccFriendStatusListener getFriendStatusListener() {
		return friendStatusListener;
	}

	public boolean isUserBlocked(String username) {
		if (blockedUsers != null) {
			for (User user : blockedUsers) {
				if (user.getUsername().equals(username)
						&& user.isModerator() != null && !user.isModerator() && user.isStaff() != null && !user.isStaff()) {
					return true;
				}
			}
		}
		if (blockingUsers != null) {
			for (User user : blockingUsers) {
				if (user.getUsername().equals(username) && !user.isModerator() && !user.isStaff()) {
					return true;
				}
			}
		}
		return false;
	}

	public int getOwnSeeksCount() {
		int ownSeeksCount = 0;
		for (Challenge challenge : ownChallenges.values()) {
			if (challenge.isSeek()) {
				ownSeeksCount++;
			}
		}
		return ownSeeksCount;
	}

	public void clearOwnChallenges() {
		ownChallenges.clear();
		//ownSeeksCount = 0;
	}
	// todo: handle creating own seek
	/*public void issue(UserSeek seek)
	  {
		if(getOwnSeeksCount() >= OWN_SEEKS_LIMIT)
		{
		  showOwnSeeksLimitMessage();
		  return;
		}
		LccUser.LOG.info("SeekConnection issue seek: " + seek);
		Challenge challenge = mapJinSeekToLccChallenge(seek);
		//outgoingLccSeeks.add(challenge);
		lccUser.getClient().sendChallenge(challenge, lccUser.getChallengeListener());
	  }*/

	public boolean isUserPlaying() {
		for (Game game : lccGames.values()) {
			if (!game.isEnded()) {
				return true;
			}
		}
		return false;
	}

	public boolean isUserPlayingAnotherGame(long currentGameId) {
		for (Game game : lccGames.values()) {
			if (!game.getId().equals(currentGameId) && !game.isEnded()) {
				return true;
			}
		}
		return false;
	}

	public void putChallenge(Long challengeId, Challenge lccChallenge) {
		challenges.put(challengeId, lccChallenge);
		androidStuff.updateChallengesList();
	}

	public void removeChallenge(long challengeId) {
		challenges.remove(challengeId);
		ownChallenges.remove(challengeId);
		androidStuff.updateChallengesList();
	}

	public void putSeek(Challenge challenge) {
		seeks.put(challenge.getId(), challenge);
		androidStuff.updateChallengesList();
	}

	public void setFriends(Collection<? extends User> friends) {
		LOG.info("CONNECTION: get friends list: " + friends);
		if (friends == null) {
			return;
		}
		for (User friend : friends) {
			putFriend(friend);
		}
	}

	public void putFriend(User friend) {
		if (friend.getStatus() != com.chess.live.client.User.Status.OFFLINE) {
			onlineFriends.put(friend.getUsername(), friend);
			this.friends.put(friend.getUsername(), friend);
		} else {
			onlineFriends.remove(friend.getUsername());
			this.friends.remove(friend.getUsername());
		}
	}

	public void removeFriend(User friend) {
		friends.remove(friend.getUsername());
		onlineFriends.remove(friend.getUsername());
	}

	public void clearOnlineFriends() {
		onlineFriends.clear();
	}

	public String[] getOnlineFriends() {
		final String[] array = new String[]{StaticData.SYMBOL_EMPTY};
		return onlineFriends.size() != 0 ? onlineFriends.keySet().toArray(array) : array;
	}

	public SubscriptionId getSeekListSubscriptionId() {
		return seekListSubscriptionId;
	}

	public void setSeekListSubscriptionId(SubscriptionId seekListSubscriptionId) {
		this.seekListSubscriptionId = seekListSubscriptionId;
	}

	public void putGame(Game lccGame) {
		lccGames.put(lccGame.getId(), lccGame);
	}

//	public Game getGame(String gameId) {
//		Log.d("TEST", "Game id = " + gameId);
//		return getGame(new Long(gameId));
//	}

	public Game getGame(long gameId) {
		return lccGames.get(gameId);
	}

	public void clearSeeks() {
		seeks.clear();
		androidStuff.updateChallengesList();
	}

	/*public void removeGame(Long id)
	  {
		lccGames.remove(id);
	  }*/

	public void clearGames() {
		lccGames.clear();
	}

	public ArrayList<GameListItem> getChallengesAndSeeksData() {
		ArrayList<GameListItem> output = new ArrayList<GameListItem>();

		Collection<Challenge> challengesAndSeeks = new ArrayList<Challenge>();
		challengesAndSeeks.addAll(challenges.values());
//		challengesAndSeeks.addAll(challenges);
		challengesAndSeeks.addAll(seeks.values());

		boolean isReleasedByMe;
		for (Challenge challenge : challengesAndSeeks) {
			String[] challengeData = new String[10];
			final User challenger = challenge.getFrom();
			isReleasedByMe = challenger.getUsername().equals(_user.getUsername());
			final GameTimeConfig challengerTimeConfig = challenge.getGameTimeConfig();
			challengeData[0] = StaticData.SYMBOL_EMPTY + challenge.getId();
			challengeData[1] = isReleasedByMe ? challenge.getTo() : challenger.getUsername();
			Integer challengerRating = 0;
			if (!isReleasedByMe) {
				switch (challengerTimeConfig.getGameTimeClass()) {
					case BLITZ: {
						challengerRating = challenger.getBlitzRating();
						break;
					}
					case LIGHTNING: {
						challengerRating = challenger.getQuickRating();
						break;
					}
					case STANDARD: {
						challengerRating = challenger.getStandardRating();
						break;
					}
				}
				if (challengerRating == null) {
					challengerRating = 0;
				}
			}
			challengeData[2] = StaticData.SYMBOL_EMPTY + challengerRating;
			String challengerChessTitle =
					challenger.getChessTitle() != null && !isReleasedByMe ? "(" + challenger.getChessTitle() + ")" : StaticData.SYMBOL_EMPTY;
			challengeData[3] = challengerChessTitle;
			String color = null;
			switch (challenge.getColor()) {
				case UNDEFINED:
					color = "0";
					break;
				case WHITE:
					color = "1";
					break;
				case BLACK:
					color = "2";
					break;
				default:
					color = "0";
					break;
			}
			challengeData[4] = color;
			challengeData[5] = challenge.isRated() ? StaticData.SYMBOL_EMPTY : "Unrated"; // is_rated

			/*int time = challengerTimeConfig.getBaseTime() * 100;
				  int hours = time / (1000 * 60 * 60);
				  time -= hours * 1000 * 60 * 60;
				  int minutes = time / (1000 * 60);*/
			challengeData[6] = (challengerTimeConfig.getBaseTime() / 10 / 60) + "min"; // base_time

			//challengeData[6] = (challengerTimeConfig.getBaseTime() / 10) + "sec"; // base_time
			challengeData[7] = challengerTimeConfig.getTimeIncrement() != 0 ?
					"+" + (challengerTimeConfig.getTimeIncrement() / 10) + "sec" : StaticData.SYMBOL_EMPTY; // time_increment
			challengeData[8] = challenge.getTo() != null ? "1" : "0"; // is_direct_challenge
			challengeData[9] = isReleasedByMe ? "1" : "0";

			output.add(new GameListItem(GameListItem.LIST_TYPE_CHALLENGES, challengeData, true));
//			output.add(new GameListItem(GameListItem.LIST_TYPE_CURRENT, challengeData, true));
		}
		return output;
	}

	public String[] getGameData(long gameId, int moveIndex) {
		Game lccGame = getGame(gameId);
		final String[] gameData = new String[GameItem.GAME_DATA_ELEMENTS_COUNT];

		gameData[0] = lccGame.getId().toString();  // TODO eliminate string conversion and use Objects
		gameData[1] = "1";
		gameData[2] = StaticData.SYMBOL_EMPTY + System.currentTimeMillis(); // todo, resolve GameListItem.TIMESTAMP
		gameData[3] = StaticData.SYMBOL_EMPTY;
		gameData[4] = lccGame.getWhitePlayer().getUsername().trim();
		gameData[5] = lccGame.getBlackPlayer().getUsername().trim();
		gameData[6] = StaticData.SYMBOL_EMPTY; // starting_fen_position
		String moves = StaticData.SYMBOL_EMPTY;
		/*int j = 0;
			int latest = 0;
			for (int i=0; j <= moveIndex; i++)
			{
			  if (lccGame.getMovesInSanNotation().charAt(i) == ' ')
			  {
				j++;
				latest = i;
			  }
			}
			if (j!=0)
			{
			  moves = lccGame.getMovesInSanNotation().substring(0, latest);
			}
			else
			{
			  moves = lccGame.getMovesInSanNotation();
			}*/

		/*String [] movesArray = lccGame.getMovesInSanNotation().split(" ");
			for (int i=0; i<=moveIndex; i++)
			{
			  moves += movesArray[i]+" ";
			}*/

		final Iterator movesIterator = lccGame.getMoves().iterator();
		for (int i = 0; i <= moveIndex; i++) {
			moves += movesIterator.next() + " ";
		}
		if (moveIndex == -1) {
			moves = StaticData.SYMBOL_EMPTY;
		}
		gameData[7] = moves; // move_list

		gameData[8] = StaticData.SYMBOL_EMPTY; // user_to_move

		Integer whiteRating = 0;
		Integer blackRating = 0;
		switch (lccGame.getGameTimeConfig().getGameTimeClass()) {
			case BLITZ: {
				whiteRating = lccGame.getWhitePlayer().getBlitzRating();
				blackRating = lccGame.getBlackPlayer().getBlitzRating();
				break;
			}
			case LIGHTNING: {
				whiteRating = lccGame.getWhitePlayer().getQuickRating();
				blackRating = lccGame.getBlackPlayer().getQuickRating();
				break;
			}
			case STANDARD: {
				whiteRating = lccGame.getWhitePlayer().getStandardRating();
				blackRating = lccGame.getBlackPlayer().getStandardRating();
				break;
			}
		}
		if (whiteRating == null) {
			whiteRating = 0;
		}
		if (blackRating == null) {
			blackRating = 0;
		}

		gameData[9] = whiteRating.toString();
		gameData[10] = blackRating.toString();

		gameData[11] = StaticData.SYMBOL_EMPTY; // todo: encoded_move_string
		gameData[12] = StaticData.SYMBOL_EMPTY; // has_new_message
		gameData[13] = StaticData.SYMBOL_EMPTY + (lccGame.getGameTimeConfig().getBaseTime() / 10); // seconds_remaining

		return gameData;
	}

	public void makeMove(long gameId, final String move) {
		final Game game = getGame(gameId);  // TODO remove final and pass like argument
		/*if(chessMove.isCastling())
			{
			  lccMove = chessMove.getWarrenSmithString().substring(0, 4);
			}
			else
			{
			  lccMove = move.getMoveString();
			  lccMove = chessMove.isPromotion() ? lccMove.replaceFirst("=", StaticData.SYMBOL_EMPTY) : lccMove;
			}*/
		final long delay = game.getOpponentClockDelay() * 100;
		synchronized (opponentClockStartSync) {
			nextOpponentMoveStillNotMade = true;
		}

		LOG.info("MOVE: making move: gameId=" + game.getId() + ", move=" + move + ", delay=" + delay);
		// TODO make outter task with argument

		/*try {*/

		new Thread(new Runnable() {
			public void run() {
				try {
					_lccClient.makeMove(game, move);
				} catch (IllegalArgumentException e) {
					Log.d("LccHolder", "Illegal move: " + move);
					/*e.printStackTrace();
					Toast.makeText(androidStuff.getContext(), "Illegal move", Toast.LENGTH_SHORT).show();*/
					// todo: still helps debugging that in market/user stacktraces
					throw new IllegalArgumentException(e);
				}
			}
		}).start();

		/*} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Log.d("ILLEGAL move", move);
		}*/

		if (game.getSeq() >= 1) // we should start opponent's clock after at least 2-nd ply (seq == 1, or seq > 1)
		{
			final boolean isWhiteRunning =
					_user.getUsername().equals(game.getWhitePlayer().getUsername());
			final ChessClock clockToBePaused = isWhiteRunning ? whiteClock : blackClock;
			final ChessClock clockToBeStarted = isWhiteRunning ? blackClock : whiteClock;
			if (game.getSeq() >= 2) // we should stop our clock if it was at least 3-rd ply (seq == 2, or seq > 2)
			{
				clockToBePaused.setRunning(false);
			}
			synchronized (opponentClockStartSync) {
				if (nextOpponentMoveStillNotMade) {
					opponentClockDelayTimer.schedule(new TimerTask() {
						@Override
						public void run() {
							synchronized (opponentClockStartSync) {
								if (nextOpponentMoveStillNotMade) {
									clockToBeStarted.setRunning(true);
								}
							}
						}
					}, delay);
				}
			}
		}
	}

	public void setNextOpponentMoveStillNotMade(boolean nextOpponentMoveStillNotMade) {
		this.nextOpponentMoveStillNotMade = nextOpponentMoveStillNotMade;
	}

	public ChessClock getBlackClock() {
		return blackClock;
	}

	public ChessClock getWhiteClock() {
		return whiteClock;
	}

	public void setWhiteClock(ChessClock whiteClock) {
		this.whiteClock = whiteClock;
	}

	public void setBlackClock(ChessClock blackClock) {
		this.blackClock = blackClock;
	}

	public void logout() {
		LOG.info("USER LOGOUT");
		androidStuff.getMainApp().setLiveChess(false);
		setCurrentGameId(null);
		setUser(null);
		androidStuff.closeLoggingInIndicator();
		androidStuff.closeReconnectingIndicator();
		getAndroidStuff().runDisconnectTask();
		setConnected(false);
		setConnectingInProgress(false);
		clearGames();
		clearChallenges();
		clearOwnChallenges();
		clearSeeks();
		clearOnlineFriends();
		setNetworkTypeName(null);
		//SessionStore.clear(androidStuff.getContext());
	}

	public boolean isConnectingInProgress() {
		return connectingInProgress;
	}

	public void setConnectingInProgress(boolean connectingInProgress) {
		this.connectingInProgress = connectingInProgress;
	}

	public boolean isSeekContains(Long id) {
		return seeks.containsKey(id);
	}

	public void removeSeek(Long id) {
		if (seeks.size() > 0) {
			seeks.remove(id);
		}
		ownChallenges.remove(id);
		androidStuff.updateChallengesList();
	}

	public Challenge getSeek(long gameId) {
		return seeks.get(gameId);
	}

	public boolean isActivityPausedMode() {
		return activityPausedMode;
	}

	public void setActivityPausedMode(boolean activityPausedMode) {
		this.activityPausedMode = activityPausedMode;
	}

	public Map<GameEvent.Event, GameEvent> getPausedActivityGameEvents() {
		return pausedActivityGameEvents;
	}

	public void processFullGame(Game game) {
		latestMoveNumber = null;
		putGame(game);
		int time = game.getGameTimeConfig().getBaseTime() * 100;
		if (whiteClock != null && whiteClock.isRunning()) {
			whiteClock.setRunning(false);
		}
		if (blackClock != null && blackClock.isRunning()) {
			blackClock.setRunning(false);
		}
		setWhiteClock(new ChessClock(this, true, time));
		setBlackClock(new ChessClock(this, false, time));
		final Activity activity = getAndroidStuff().getGameActivity();
		if (activity != null) {
			activity.finish();
		}
		final ContextWrapper androidContext = androidStuff.getMainApp();

		final Intent intent = new Intent(androidContext, GameLiveScreenActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(AppConstants.GAME_MODE, AppConstants.GAME_MODE_LIVE_OR_ECHESS);
		intent.putExtra(GameListItem.GAME_ID, game.getId());
		androidContext.startActivity(intent);
		/*final Game currentGame = game;
			if(game.getSeq() > 0)
			{
			  androidStuff.getUpdateBoardHandler().postDelayed(new Runnable()
			  {
				public void run()
				{
				  doReplayMoves(currentGame);
				}
			  }, 2000); // todo: remove delay, change logic to use SerializableExtra() probably, process moves replay on the Game activity
			}*/
	}

	public void doReplayMoves(Game game) {
		LOG.info("GAME LISTENER: replay moves,  gameId " + game.getId());
		//final String[] sanMoves = game.getMovesInSanNotation().trim().split(" ");
		final List<String> coordMoves = new ArrayList<String>(game.getMoves());
		User whitePlayer = game.getWhitePlayer();
		User blackPlayer = game.getBlackPlayer();
		User moveMaker;
		for (int i = 0; i < coordMoves.size(); i++) {
			moveMaker = (i % 2 == 0) ? whitePlayer : blackPlayer;
			doMoveMade(game, moveMaker, coordMoves.get(i), i);
		}
	}

	public void doMoveMade(final Game game, final User moveMaker, String move, /*boolean isNew,*/ int moveIndex) {
		/*if(move.length() == 5)
			{
			  final String promotionSign = move.substring(4, 5);
			  promotion = game.getWhitePlayer().getUsername().equals(moveMaker.getUsername()) ?
						  variant.parsePiece(promotionSign.toUpperCase()) : variant.parsePiece(promotionSign);
			}
			else
			{
			  promotion = null;
			}*/
		//final String sanMove = game.getMovesInSanNotation().trim().split(" ")[moveIndex];
		// todo

		if (((latestMoveNumber != null) && (moveIndex < latestMoveNumber)) || (latestMoveNumber == null && moveIndex > 0)) {
			LOG.info("GAME LISTENER: Extra onMoveMade received (currentMoveIndex=" + moveIndex + ", latestMoveNumber=" + latestMoveNumber + ")");
			return;
		} else {
			latestMoveNumber = moveIndex;
		}
		if (isActivityPausedMode()) {
			final GameEvent moveEvent = new GameEvent();
			moveEvent.setEvent(GameEvent.Event.Move);
			moveEvent.setGameId(game.getId());
			moveEvent.setMoveIndex(moveIndex);
			getPausedActivityGameEvents().put(moveEvent.getEvent(), moveEvent);
		} else {
			androidStuff.processMove(game.getId(), moveIndex);
		}
		doUpdateClocks(game, moveMaker, moveIndex);
	}

	private void doUpdateClocks(Game game, User moveMaker, int moveIndex) {
		// TODO: This method does NOT support the game observer mode. Redevelop it if necessary.
//		setClockDrawPointer(!game.getWhitePlayer().getUsername().equals(moveMaker.getUsername()));

		if (game.getSeq() >= 2 && moveIndex == game.getSeq() - 1) {
			final boolean isOpponentMoveDone = !_user.getUsername().equals(moveMaker.getUsername());

			if (isOpponentMoveDone) {
				synchronized (opponentClockStartSync) {
					setNextOpponentMoveStillNotMade(false);
				}
			}
			//final boolean amIWhite = _user.getUsername().equals(game.getWhitePlayer().getUsername());
			/*final boolean updateWhite = isOpponentMoveDone || amIWhite;
				  final boolean updateBlack = isOpponentMoveDone || !amIWhite;*/
			final boolean isWhiteDone = game.getWhitePlayer().getUsername().equals(moveMaker.getUsername());
			final boolean isBlackDone = game.getBlackPlayer().getUsername().equals(moveMaker.getUsername());
			final int whitePlayerTime = game.getActualClockForPlayer(game.getWhitePlayer()).intValue() * 100;
			final int blackPlayerTime = game.getActualClockForPlayer(game.getBlackPlayer()).intValue() * 100;


            getWhiteClock().setTime(whitePlayerTime);
			if (!game.isEnded()) {
                getWhiteClock().setRunning(isBlackDone);
			}

            getBlackClock().setTime(blackPlayerTime);
			if (!game.isEnded()) {
                getBlackClock().setRunning(isWhiteDone);
			}

		}
	}

	public void updateClockTime(Game game) {
		/*int whitePlayerTime = game.getActualClockForPlayer(game.getWhitePlayer()).intValue() * 100;
			int blackPlayerTime = game.getActualClockForPlayer(game.getBlackPlayer()).intValue() * 100;
			System.out.println("!!!!!!!!!!!!!!!!!!!! WHITE TIME " + getWhiteClock().createTimeString(whitePlayerTime));
			System.out.println("!!!!!!!!!!!!!!!!!!!! BLACK TIME " + getBlackClock().createTimeString(blackPlayerTime));
			getWhiteClock().setTime(whitePlayerTime);
			getBlackClock().setTime(blackPlayerTime);*/
	}

	public void setCurrentGameId(Long gameId) {
		currentGameId = gameId;
	}

	public Long getCurrentGameId() {
		return currentGameId;
	}

	public void putGameChat(Long gameId, Chat chat) {
		gameChats.put(gameId, chat);
	}

	public Chat getGameChat(Long gameId) {
		return gameChats.get(gameId);
	}

	public LinkedHashMap<Chat, LinkedHashMap<Long, ChatMessage>> getReceivedChats() {
		return receivedChatMessages;
	}

	public LinkedHashMap<Long, ChatMessage> getChatMessages(String chatId) {
		for (Chat storedChat : receivedChatMessages.keySet()) {
			if (chatId.equals(storedChat.getId())) {
				return receivedChatMessages.get(storedChat);
			}
		}
		return null;
	}

	public void setNetworkTypeName(String networkTypeName) {
		this.networkTypeName = networkTypeName;
	}

	public String getNetworkTypeName() {
		return networkTypeName;
	}

	public Boolean isFairPlayRestriction(long gameId) {
		Log.d("TEST","gameId = " + gameId);
		Game game = getGame(gameId);
		try {
			if (game.getWhitePlayer().getUsername().equals(_user.getUsername()) && !game.isAbortableByWhitePlayer()) {
				return true;
			}
			if (game.getBlackPlayer().getUsername().equals(_user.getUsername()) && !game.isAbortableByBlackPlayer()) {
				return true;
			}
		}catch (NullPointerException e) {
			// helps debug issue
			String message = "gameId=" + gameId + ", game != null " + (game != null) + ", _user" + _user;
			if (_user != null) {
				message +=  ", username=" + _user.getUsername();
			}
			throw new NullPointerException(message);
		}

		return false;
	}

	public Boolean isAbortableBySeq(long gameId) {
		return getGame(gameId).getSeq() < 3;
	}

	public void setOuterChallengeListener(OuterChallengeListener outerChallengeListener) {
		challengeListener.setOuterChallengeListener(outerChallengeListener);
	}

	public void setExternalConnectionListener(LccConnectionListener externalConnectionListener) {
		this.externalConnectionListener = externalConnectionListener;
	}

	public void updateConnectionState() {
		externalConnectionListener.onConnected(connected);
	}

	public void declineAllChallenges(Challenge acceptedChallenge) {
		// decline all challenges except acceptedChallenge
		List<Challenge> removeMe = new ArrayList<Challenge>();
		for (Challenge challenge : challenges.values()) {
			if(!challenge.equals(acceptedChallenge))
				removeMe.add(challenge);
		}
		Challenge[] declinedChallenges = new Challenge[removeMe.size()];
		for (int i = 0, removeMeSize = removeMe.size(); i < removeMeSize; i++) {
			Challenge challenge = removeMe.get(i);
			declinedChallenges[i] = challenge;
		}

		getAndroidStuff().runRejectBatchChallengeTask(declinedChallenges);
		challengeListener.getOuterChallengeListener().hidePopups();
	}

	public void declineCurrentChallenge(Challenge currentChallenge) {
		getAndroidStuff().runRejectChallengeTask(currentChallenge);
		final List<Challenge> retainMe = new ArrayList<Challenge>();
		for (Challenge challenge : challenges.values()) {
			if(!challenge.equals(currentChallenge))
				 retainMe.add(challenge);
		}

		if (retainMe.size() > 0)
			challengeListener.getOuterChallengeListener().showDelayedDialog(retainMe.get(retainMe.size() - 1));
	}

	public Context getContext() {
		return context;
	}
}
