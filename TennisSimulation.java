import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import java.io.FileReader;
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

class Player {
    int id;
    String hand;
    int experience;
    Map<String, Integer> skills;

    Player(int playerId, String playerHand, int playerExperience, Map<String, Integer> playerSkills) {
        id = playerId;
        hand = playerHand;
        experience = playerExperience;
        skills = playerSkills;
    }
}

class Tournament {
    int id;
    String surface;
    String type;

    Tournament(int tournamentId, String tournamentSurface, String tournamentType) {
        id = tournamentId;
        surface = tournamentSurface;
        type = tournamentType;
    }
}

class MatchResult {
    int winnerId;
    int loserId;

    MatchResult(int winner, int loser) {
        winnerId = winner;
        loserId = loser;
    }
}

class PlayersPair {
    Player player1;
    Player player2;
}

public class TennisSimulation {

    public static void main(String[] args) {
        List<Player> players = new ArrayList<>();
        List<Tournament> tournaments = new ArrayList<>();
        List<MatchResult> matchResults = new ArrayList<>();
        List<Player> winners = new ArrayList<>();

        // Read player and tournament data from JSON file
        parseJSON("input.json", players, tournaments);

        // Simulate each tournament and determine the winner
        for (Tournament tournament : tournaments) {
            if (tournament.type.equals("elimination")) {
                winners.add(runEliminationTournament(tournament, players));
            } else if (tournament.type.equals("league")) {
                runLeagueTournament(tournament, players, matchResults);
            }
        }

        // Calculate total experience for each player
        Map<Integer, Integer> playerTotalExperience = new HashMap<>();
        for (Player player : players) {
            playerTotalExperience.put(player.id, player.experience);
        }
        for (MatchResult result : matchResults) {
            playerTotalExperience.put(result.winnerId, playerTotalExperience.get(result.winnerId) + 10);
            playerTotalExperience.put(result.loserId, playerTotalExperience.get(result.loserId) + 1);
        }

        // Sort players by their total experience in descending order
        List<Map.Entry<Integer, Integer>> sortedPlayers = new ArrayList<>(playerTotalExperience.entrySet());
        sortedPlayers.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed());

        // Prepare the final JSON output
        JSONArray resultsArray = new JSONArray();
        int order = 1;
        for (Map.Entry<Integer, Integer> entry : sortedPlayers) {
            int playerId = entry.getKey();
            int gainedExperience = playerTotalExperience.get(playerId) - players.get(playerId - 1).experience;
            int totalExperience = playerTotalExperience.get(playerId);

            JSONObject playerResult = new JSONObject();
            playerResult.put("order", order);
            playerResult.put("player_id", playerId);
            playerResult.put("gained_experience", gainedExperience);
            playerResult.put("total_experience", totalExperience);

            resultsArray.add(playerResult);
            order++;
        }

        JSONObject finalResult = new JSONObject();
        finalResult.put("results", resultsArray);

        // Create an output file
        writeJSONToFile(finalResult.toJSONString());
    }

    private static void parseJSON(String filename, List<Player> players, List<Tournament> tournaments) {
        JSONParser jsonParser = new JSONParser();

        try {
            // Read JSON file
            Object obj = jsonParser.parse(new FileReader(filename));
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray playersArray = (JSONArray) jsonObject.get("players");
            JSONArray tournamentsArray = (JSONArray) jsonObject.get("tournaments");

            // Parse player data and add players to the list
            for (int i = 0; i < playersArray.size(); i++) {
                JSONObject object = (JSONObject) playersArray.get(i);
                int id = ((Long) object.get("id")).intValue();
                String hand = (String) object.get("hand");
                int experience = ((Long) object.get("experience")).intValue();
                JSONObject skillsObject = (JSONObject) object.get("skills");

                Map<String, Integer> skills = new HashMap<>();
                skills.put("clay", ((Long) skillsObject.get("clay")).intValue());
                skills.put("grass", ((Long) skillsObject.get("grass")).intValue());
                skills.put("hard", ((Long) skillsObject.get("hard")).intValue());

                Player player = new Player(id, hand, experience, skills);
                players.add(player);
            }

            // Parse tournament data and add tournaments to the list
            for (int k = 0; k < tournamentsArray.size(); k++) {
                JSONObject tobject = (JSONObject) tournamentsArray.get(k);
                int sid = ((Long) tobject.get("id")).intValue();
                String surface = (String) tobject.get("surface");
                String type = (String) tobject.get("type");

                Tournament tournament = new Tournament(sid, surface, type);
                tournaments.add(tournament);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeJSONToFile(String jsonData) {
        try (FileWriter fileWriter = new FileWriter("output.json")) {
            fileWriter.write(jsonData);
            System.out.println("JSON data has been written to output.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runLeagueTournament(Tournament tournament, List<Player> players,
            List<MatchResult> matchResults) {
        List<Player> tournamentPlayers = new ArrayList<>(players);
        List<PlayersPair> pairPlayers = new ArrayList<>();

        // Generate all possible pairs of players for the tournament
        for (int i = 0; i < tournamentPlayers.size(); i++) {
            for (int j = i + 1; j < tournamentPlayers.size(); j++) {
                PlayersPair tempPair = new PlayersPair();
                tempPair.player1 = tournamentPlayers.get(i);
                tempPair.player2 = tournamentPlayers.get(j);
                pairPlayers.add(tempPair);
            }
        }

        while (pairPlayers.size() > 0) {
            // Randomly select a pair of players from the tournamentPlayers list
            int randomIndex = (int) (Math.random() * pairPlayers.size());
            PlayersPair playersPair = pairPlayers.get(randomIndex);
            Player player1 = playersPair.player1;
            Player player2 = playersPair.player2;

            // Remove the selected pair of players from the tournamentPlayers list
            pairPlayers.remove(randomIndex);

            // Simulate the match between player1 and player2 and determine the winner
            int player1Score = calculatePlayerScore(player1, tournament.surface);
            int player2Score = calculatePlayerScore(player2, tournament.surface);

            // Determine the match winner based on the scoring system and update their
            // experience scores
            if (player1Score > player2Score) {
                player1.experience += 10;
                player2.experience += 1;
                matchResults.add(new MatchResult(player1.id, player2.id));
            } else {
                player2.experience += 10;
                player1.experience += 1;
                matchResults.add(new MatchResult(player2.id, player1.id));
            }

            // Update the experience scores of the players in the players list
            int index1 = players.indexOf(player1);
            int index2 = players.indexOf(player2);
            players.set(index1, player1);
            players.set(index2, player2);
        }
    }

    // Calculate the score of a player in a league match based on the scoring system
    private static int calculatePlayerScore(Player player, String surface) {
        int score = 0;
        score += 1; // Match score
        if (player.hand.equals("left"))
            score += 2; // Left-hand bonus score
        score += player.experience > 0 ? 3 : 0; // Experience score
        score += player.skills.get(surface) > 0 ? 4 : 0; // Surface skill score
        return score;
    }

    private static void sortPlayersByExperience(List<Player> players) {
        players.sort((player1, player2) -> {
            if (player1.experience != player2.experience) {
                return player2.experience - player1.experience;
            } else {
                return player1.id - player2.id;
            }
        });
    }

    private static Player runEliminationTournament(Tournament tournament, List<Player> players) {
        List<Player> tournamentPlayers = new ArrayList<>(players);
        List<Player> winnerPlayers = new ArrayList<>();

        // Continue the elimination tournament until there is only one player left
        while (tournamentPlayers.size() > 1) {
            List<Player> winners = new ArrayList<>();

            // Simulate matches between pairs of players in each round
            for (int i = 0; i < tournamentPlayers.size(); i += 2) {
                Player player1 = tournamentPlayers.get(i);
                Player player2 = tournamentPlayers.get(i + 1);

                // Calculate the scores of player1 and player2 in the match based on the scoring
                // system
                int player1Score = calculatePlayerScore(player1, tournament.surface);
                int player2Score = calculatePlayerScore(player2, tournament.surface);

                // Determine the match winner based on the scoring system and update their
                // experience scores
                if (player1Score > player2Score) {
                    winners.add(player1);
                    player1.experience += 20;
                    player2.experience += 10;
                } else {
                    winners.add(player2);
                    player2.experience += 20;
                    player1.experience += 10;
                }
            }

            // Update the tournamentPlayers list to contain only the winners of the current
            // round
            tournamentPlayers = winners;
        }

        // At the end of the elimination tournament, there will be only one player left,
        // the winner
        winnerPlayers.addAll(tournamentPlayers);

        // Update the players list with the updated experience scores of the winners
        for (Player winner : winnerPlayers) {
            int index = players.indexOf(winner);
            players.set(index, winner);
        }

        // If there is more than one winner, run the elimination tournament again with
        // the winners
        if (winnerPlayers.size() > 1) {
            return runEliminationTournament(tournament, winnerPlayers);
        } else {
            return winnerPlayers.get(0); // Return the final winner
        }
    }
}
