package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    // Main Command
    public Command run() {
        Worm enemyWorm;
        if (currentWorm.profession == "Commando") {
            // untuk select menggunakan snowball atau banana jika tersedia, baru banana belum snowball
            int idxEnemyAgent =  isAnyEnemyThrowable("Agent");

            if(idxEnemyAgent != -1){
                if (gameState.myPlayer.worms[1].bananaBombs.count > 0) {
                    Command Banana = new BananaCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                    return new  SelectCommand(2, Banana.render());
                } 
            } 
        }else{
            // untuk banana atau snowball, jika masih ada peluru tembak terus pake select
            int idxEnemyThrowable =  isAnyEnemyThrowable(currentWorm.profession);
            if(idxEnemyThrowable != -1){

            
            if (currentWorm.bananaBombs != null) {
                if (currentWorm.bananaBombs.count > 0) {
                    return new BananaCommand(opponent.worms[idxEnemyThrowable].position.x, opponent.worms[idxEnemyThrowable].position.y);
                } 
            } else if (currentWorm.snowBalls != null) {
                if (currentWorm.snowBalls.count > 0 && opponent.worms[idxEnemyThrowable].roundsUntilUnfrozen == 0) {
                    return new SnowCommand(opponent.worms[idxEnemyThrowable].position.x, opponent.worms[idxEnemyThrowable].position.y);
                } 
            }
        }
        }
       
        enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }
        // List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        // int cellIdx = random.nextInt(surroundingBlocks.size());
        
        Position powerup = getNearestPowerupPosition();
        // Prioritas ambil powerUp jika health tidak max
        if (powerup.x != -1 && currentWorm.health < 100) {
            return digAndMoveTo(currentWorm.position, powerup);
        } else{
            enemyWorm = getWormToHunt();
            return digAndMoveTo(currentWorm.position, enemyWorm.position);
        }
        
        // Cell block = getNextCellToGo(currentWorm.position, enemyWorm.position);

        // if (block.type == CellType.AIR) {
        //     return new MoveCommand(block.x, block.y);
        // } else if (block.type == CellType.DIRT) {
        //     return new DigCommand(block.x, block.y);
        // }

        // return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                /* enemyWorm.health > 0 : cek dulu musuhnya masih hidup apa kagak*/
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }
        
        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    /* Cari cell selanjutnya yang mengarah ke tujuan (destination) */
    private Cell getNextCellToGo(Position origin, Position destination) {
        /* list semua cell di sekitar, 
        setiap cellnya akan dihitung jarak euclidean ke dest,*/
        List<Cell> surroundingBlocks = getSurroundingCells(origin.x, origin.y);
        int size = surroundingBlocks.size();
        int[] arrDistance = new int[size];

        /* pilih cell yang jaraknya paling minimum */
        int imin = 0;
        for (int i = 0; i < size; i++) {
            Cell block = surroundingBlocks.get(i);
            arrDistance[i] = euclideanDistance(block.x, block.y, destination.x, destination.y);
            if (arrDistance[i] < arrDistance[imin]) {
                imin = i;
            }
        }
        
        return surroundingBlocks.get(imin);
    }

    private Worm getWormToHunt() {
        if (opponent.worms[0].health > 0) {
            return opponent.worms[0];
        } else if (opponent.worms[1].health > 0) {
            return opponent.worms[1];
        } else if (opponent.worms[2].health > 0) {
            return opponent.worms[2];
        } else {
            return null;
        }
    }

    // Utility = ganti profesion
    // worm.profession
    private boolean isEnemyThrowable(Position enemy_position , String profession) { //randy
        /** cek di sekitar musuh ada temen atau ngga
         * profession bisa berupa "Agent" atau "Technologist"
         */
        for (int i = 0; i < 3; ++i) {
            if (gameState.myPlayer.worms[i].health != 0) {
                /** kalau ga mati berarti ada kemungkinan deket musuh **/
                Position myWormPosition = gameState.myPlayer.worms[i].position;
                int radius = 0; // default value , kalau commando
                if (profession == "Agent"){
                    radius = 2;
                }else if (profession == "Technologist") {
                    radius = 1;
                }
                if (euclideanDistance(myWormPosition.x, myWormPosition.y, enemy_position.x, enemy_position.y) <= radius || radius == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private Position getNearestPowerupPosition() {
        /**mencari letak powerup terdekat */
        List<Position> allPowerUp = new ArrayList<>();
        boolean thereIsPowerUp = false;
        for (int i = 0; i < gameState.mapSize; ++i) {
            for (int j = 0; j < gameState.mapSize; ++j) {
                if (gameState.map[i][j].powerUp != null) {
                    Position powerupPosition = new Position();
                    powerupPosition.x = i;
                    powerupPosition.y = j;
                    allPowerUp.add(powerupPosition);
                    thereIsPowerUp = true;
                }
            }
        }
        if (thereIsPowerUp) {
            int absisWorm = currentWorm.position.x;
            int ordinatWorm = currentWorm.position.y;
            Position minimumPoint = allPowerUp.get(0);
            int minimumDistance = euclideanDistance(minimumPoint.x, minimumPoint.y, absisWorm, ordinatWorm);
            for (int k = 0; k < allPowerUp.size() ; ++k) {
                Position powerUpPosition = allPowerUp.get(k);
                int distancePowerUp = euclideanDistance(powerUpPosition.x, powerUpPosition.y, absisWorm, ordinatWorm);
                if (distancePowerUp < minimumDistance) {
                    minimumDistance = distancePowerUp;
                    minimumPoint = powerUpPosition;
                }
            }
            return minimumPoint;
        }
        /**kalau gaada powerup lagi, return position dengan absis -1 dan ordinat -1 */

        Position nothing = new Position();
        nothing.x = -1; nothing.y = -1;
        return nothing;
    }

    private boolean isEnemyFrozen(Worm enemy){
        return (enemy.roundsUntilUnfrozen > 0);
    }

    // Origin --> titik asal
    private Command digAndMoveTo(Position Origin, Position destination){
        Cell block = getNextCellToGo(Origin, destination);
        
        if(block.type == CellType.DIRT){
            return new DigCommand(block.x, block.y);
        } else if (block.type == CellType.AIR){
            return new MoveCommand(block.x, block.y);
        } else{
            return new DoNothingCommand();
        }
    }

    // Mengembalikan worm ID yang masih punya ultimate or both
    private int stillHaveUltimate(){
        // 3 jika keduanya masih punya
        if(gameState.myPlayer.worms[1].bananaBombs.count > 0 && gameState.myPlayer.worms[2].snowBalls.count > 0){
            return 1;
        } else if(gameState.myPlayer.worms[1].bananaBombs.count > 0){
            return 2;
        } else{ // technologist
            return 3;
        }
    }
   
    private int isAnyEnemyThrowable(String profession) {
        // mengembalikan idx worm yang bisa dilempar
        // mengembalikan -1 jika tidak ada 
        for (int i = 0; i < 3; i++) {
            if (isEnemyThrowable(opponent.worms[i].position, profession)) {
                return i;
            }
        }
        
        return -1;
    }

    private boolean isToNearFriend(){
        //mengembalikan true jika radius antar worm terlalu dekat (radius antar teman < 3)
        //mengembalikan false jika radius antar worm masih batas wajar (radius antar teman >= 3)
        for (int i = 0; i < 3; ++i){
            MyWorm wormI = gameState.myPlayer.worms[i];
            if ( wormI != currentWorm){
                int radiusBetweenFriend = euclideanDistance(wormI.position.x, wormI.position.y, currentWorm.position.x, currentWorm.position.y);
                if (radiusBetweenFriend < 3){
                    return true;
                }
            }
        }
        return false;
    }
   
    
   
   
   
   
   
   
   
   
   
   
   
   
   
   
   
}
