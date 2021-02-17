package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.*;

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
        /* Memeriksa apakah worm yang sedang dipilih, terkena effect freeze atau tidak */
        boolean selected = false;
        MyWorm[] meWorm = gameState.myPlayer.worms;
        int index =0;
        if(isWormFrozen(currentWorm) && gameState.myPlayer.remainingWormSelections > 0){
            for(int i =0; i < 3; i++){
                if(!isWormFrozen(meWorm[i]) && meWorm[i].health > 0){
                    selected = true;
                    /* ID bisa digunakan untuk select Command, menunjukkan ID worm yang tidak terkena effect freeze */
                    index = i;
                    break;
                }
            }
        }
        
        if (selected) {
            /* Gunakan command select */
            /* Jika current worm merupakan commando, cek apakah worm lain dapat menembakkan skill kepada musuh atau tidak*/
            int idxEnemyAgent = isAnyEnemyThrowable("Agent");
            if(meWorm[index].profession.equals("Commando") && gameState.myPlayer.remainingWormSelections > 0){
                if(idxEnemyAgent != -1){
                    if (IsWormCanThrow(gameState.myPlayer.worms[1])) {
                        Command Banana = new BananaCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                        return new SelectCommand(2, Banana.render());
                    } 
                }
                idxEnemyAgent = isAnyEnemyThrowable("Technologist");
                if(idxEnemyAgent != -1){
                    /* Periksa apakah masih ada peluru dan tidak sedang terkena freeze */
                    if (IsWormCanThrow(gameState.myPlayer.worms[2])){
                        Command snowball = new SnowCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                        return new SelectCommand(3, snowball.render());
                    } 
                }
            }

            /* Select banana weapon */
            if(meWorm[index].bananaBombs != null && meWorm[index].bananaBombs.count > 0 && idxEnemyAgent != -1){
                Command Banana = new BananaCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                return new SelectCommand(index+1, Banana.render());
            }

            /* Select freeze weapon */
            idxEnemyAgent = isAnyEnemyThrowable("Technologist");
            if(meWorm[index].snowBalls != null && meWorm[index].snowBalls.count > 0 && idxEnemyAgent != -1 && !isWormFrozen(opponent.worms[idxEnemyAgent])){
                Command snowball = new SnowCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                return new SelectCommand(index+1, snowball.render());
            }

            /* Tembakan basic */
            Worm enemyWorm = getFirstWormInRange(meWorm[index]);
            if (enemyWorm != null) {
                Direction direction = resolveDirection(meWorm[index].position, enemyWorm.position);
                Command Shoot = new ShootCommand(direction);
                return new SelectCommand(index+1, Shoot.render());
            }

            enemyWorm = getWormToHunt(index+1);
            return digAndMoveTo(currentWorm.position, enemyWorm.position); 
            // return new SelectCommand(index+1, digAndMoveTo(meWorm[index].position, enemyWorm.position).render()); 
        } else {
            /* Jika current worm merupakan commando, cek apakah worm lain dapat menembakkan skill kepada musuh atau tidak*/
            int idxEnemyAgent = isAnyEnemyThrowable("Agent");
            if(currentWorm.profession.equals("Commando") && gameState.myPlayer.remainingWormSelections > 0){
                if(idxEnemyAgent != -1){
                    if (IsWormCanThrow(gameState.myPlayer.worms[1])) {
                        Command Banana = new BananaCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                        return new SelectCommand(2, Banana.render());
                    } 
                }
                idxEnemyAgent = isAnyEnemyThrowable("Technologist");
                if(idxEnemyAgent != -1){
                    /* Periksa apakah masih ada peluru dan tidak sedang terkena freeze */
                    if (IsWormCanThrow(gameState.myPlayer.worms[2])){
                        Command snowball = new SnowCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                        return new SelectCommand(3, snowball.render());
                    } 
                }
            }

            /* Select banana weapon dahulu */
            if(currentWorm.bananaBombs != null && currentWorm.bananaBombs.count > 0 && idxEnemyAgent != -1){
                return new BananaCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
            }

            /* Select freeze weapon */
            idxEnemyAgent = isAnyEnemyThrowable("Technologist");
            if(currentWorm.snowBalls != null && currentWorm.snowBalls.count > 0 && idxEnemyAgent != -1 && !isWormFrozen(opponent.worms[idxEnemyAgent])){
                return new SnowCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
            }

            /* Tembakan basic */
            Worm enemyWorm = getFirstWormInRange(currentWorm);
            if (enemyWorm != null) {
                Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
                return new ShootCommand(direction);
            }

            enemyWorm = getWormToHunt(currentWorm.id);
            return digAndMoveTo(currentWorm.position, enemyWorm.position);            
        }
    }

    /* Mengembalikan posisi musuh terdekat dengan selected worm,
    dengan parameter Myworm. Jika tidak ada musuh kembalikan NULL */
    private Worm getFirstWormInRange(MyWorm worm) {

        Set<String> cells = constructFireDirectionLines(worm.weapon.range, worm)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0 ) {
                /* enemyWorm.health > 0 : cek dulu musuhnya masih hidup apa kagak*/
                return enemyWorm;
            }  
        }
        return null;
    }


    private List<List<Cell>> constructFireDirectionLines(int range, MyWorm worm) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = worm.position.x + (directionMultiplier * direction.x);
                int coordinateY = worm.position.y + (directionMultiplier * direction.y);

                /* Cek apakah kordinat valid */
                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                /* Cek apakah masih berada pada jarak tembak cellnya atau tidak */
                if (euclideanDistance(worm.position.x, worm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                /* Jika ada teman pada fire direction lines, stop proses */
                boolean isFriend = false;
                for (Worm friendWorm : gameState.myPlayer.worms) {
                    if (friendWorm.position.x == cell.x && friendWorm.position.y == cell.y) {
                        isFriend = true;
                    }
                }

                if(isFriend){
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

    private boolean isCellPowerUp(Cell my_cell) {
        return (my_cell.powerUp != null);
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
            //prior
            if (isCellPowerUp(block)) { //powerup pasti di block air
                return block;
            }
            arrDistance[i] = euclideanDistance(block.x, block.y, destination.x, destination.y);
            if (gameState.currentRound < 150) { // belum ada lava
                if (arrDistance[i] < arrDistance[imin] && !isToNearFriend(block)) {
                    imin = i;
                }
            }else{ //round >= 150 mulai ada lava, ini bisa diganti angka roundnya
                if (arrDistance[i] < arrDistance[imin]) {
                    imin = i;
                }
            }
        }
        return surroundingBlocks.get(imin);
    }



    /* Target enemy berdasarkan ID */
    private Worm getWormToHunt(int ID) {
        if(ID == 1){
            if (opponent.worms[2].health > 0) {
                return opponent.worms[2];
            } else if (opponent.worms[0].health > 0) {
                return opponent.worms[0];
            } else if (opponent.worms[1].health > 0) {
                return opponent.worms[1];
            } else {
                return null;
            }
        } else{
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

    }

    private boolean isEnemyThrowable(Position enemy_position , String profession) { //randy
        /** cek di sekitar musuh ada temen atau ngga
         * profession bisa berupa "Agent" atau "Technologist"
         */
        for (int i = 0; i < 3; ++i) {
            if (gameState.myPlayer.worms[i].health > 0) {
                /** kalau ga mati berarti ada kemungkinan deket musuh **/
                Position myWormPosition = gameState.myPlayer.worms[i].position;
                int radius = 0; // default value , kalau commando
                if (profession.equals("Agent")){
                    radius = 2;
                }else if (profession.equals("Technologist")) {
                    radius = 1;
                }
                if (euclideanDistance(myWormPosition.x, myWormPosition.y, enemy_position.x, enemy_position.y) <= radius || radius == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    // private Position getNearestPowerupPosition(MyWorm worm) {
    //     /**mencari letak powerup terdekat */
    //     List<Position> allPowerUp = new ArrayList<>();
    //     boolean thereIsPowerUp = false;
    //     for (int i = 0; i < gameState.mapSize; ++i) {
    //         for (int j = 0; j < gameState.mapSize; ++j) {
    //             if (gameState.map[i][j].powerUp != null) {
    //                 Position powerupPosition = new Position();
    //                 powerupPosition.x = i;
    //                 powerupPosition.y = j;
    //                 allPowerUp.add(powerupPosition);
    //                 thereIsPowerUp = true;
    //             }
    //         }
    //     }
    //     if (thereIsPowerUp) {
    //         int absisWorm = worm.position.x;
    //         int ordinatWorm = worm.position.y;
    //         Position minimumPoint = allPowerUp.get(0);
    //         int minimumDistance = euclideanDistance(minimumPoint.x, minimumPoint.y, absisWorm, ordinatWorm);
    //         for (int k = 0; k < allPowerUp.size() ; ++k) {
    //             Position powerUpPosition = allPowerUp.get(k);
    //             int distancePowerUp = euclideanDistance(powerUpPosition.x, powerUpPosition.y, absisWorm, ordinatWorm);
    //             if (distancePowerUp < minimumDistance) {
    //                 minimumDistance = distancePowerUp;
    //                 minimumPoint = powerUpPosition;
    //             }
    //         }
    //         return minimumPoint;
    //     }
        /**kalau gaada powerup lagi, return position dengan absis -1 dan ordinat -1 */

    //     Position nothing = new Position();
    //     nothing.x = -1; nothing.y = -1;
    //     return nothing;
    // }

    private boolean isWormFrozen(Worm worm){
        return (worm.roundsUntilUnfrozen > 0);
    }

    private boolean IsWormCanThrow(MyWorm worm){
        if(worm.health <= 0){
            return false;
        }

        if(worm.bananaBombs != null){
            return (worm.roundsUntilUnfrozen > 0 && worm.bananaBombs.count > 0);
        } else{
            return (worm.roundsUntilUnfrozen > 0 && worm.snowBalls.count > 0);
        }     
    }

    // Origin --> titik asal
    private Command digAndMoveTo(Position Origin, Position destination){
        // Cell block = getNextCellToGo(Origin, destination);
        Cell block;
        if (euclideanDistance(Origin.x, Origin.y, destination.x, destination.y) > 7 ) {
            block = getNextCellToGo(Origin, destination);
        } else {
            block = getNextCellToAttack(Origin, destination);
        }

        if (block != null) {
            if(block.type == CellType.DIRT){ 
                return new DigCommand(block.x, block.y);
            } else if (block.type == CellType.AIR){
                return new MoveCommand(block.x, block.y);
            }
        } 
        return new DoNothingCommand();
    }

    // Mengembalikan worm ID yang masih punya ultimate or both
//    private int stillHaveUltimate(){
//        // 3 jika keduanya masih punya
//        if(gameState.myPlayer.worms[1].bananaBombs.count > 0 && gameState.myPlayer.worms[2].snowBalls.count > 0){
//            return 1;
//        } else if(gameState.myPlayer.worms[1].bananaBombs.count > 0){
//            return 2;
//        } else{ // technologist
//            return 3;
//        }
//    }
   
    private int isAnyEnemyThrowable(String profession) {
        // mengembalikan idx worm yang bisa dilempar
        // mengembalikan -1 jika tidak ada 
        int idWorm = 0;
        int range = 5;
        if (profession.equals("Agent")) {
            idWorm = 1;
        } else if (profession.equals("Technologist")) {
            idWorm = 2;
            range = 4;
        }    

        for (int i = 0; i < 3; i++) {
            if (isEnemyInThrowRange(opponent.worms[i].position, gameState.myPlayer.worms[idWorm].position, range)) {
                if (isEnemyThrowable(opponent.worms[i].position, profession)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isToNearFriend(Cell nextCell){
        //mengembalikan true jika radius antar worm terlalu dekat (radius antar teman < 3)
        //mengembalikan false jika radius antar worm masih batas wajar (radius antar teman >= 3)
        for (int i = 0; i < 3; ++i){
            MyWorm wormI = gameState.myPlayer.worms[i];
            if (wormI.id != currentWorm.id && wormI.health > 0){
                int radiusBetweenFriend = euclideanDistance(wormI.position.x, wormI.position.y, nextCell.x, nextCell.y);
                if (radiusBetweenFriend < 3){
                    return true;
                }
            }
        }
        return false;
    }
   
    private boolean isEnemyInThrowRange(Position enemyPosition, Position launcher, int range) {
        // memeriksa apakah enemyPosition berada pada jarak lemparan
        return euclideanDistance(launcher.x, launcher.y, enemyPosition.x, enemyPosition.y) <= range;
    }
   

    private Cell getNextCellToAttack(Position origin, Position destination) {
        /* list semua cell di sekitar, 
        setiap cellnya akan dihitung jarak euclidean ke dest,*/
        List<Cell> surroundingBlocks = getSurroundingCells(origin.x, origin.y);
        List<Cell> attackingBlocks = getAttackingCells(destination.x, destination.y);

        int minimumDistance = euclideanDistance(origin.x, origin.y, destination.x, destination.y);
        int nextX = origin.x;
        int nextY = origin.y;

        /* pilih cell yang jaraknya paling minimum */
        // int imin = 0;
        // for (int i = 0; i < size; i++) {
        //     Cell block = surroundingBlocks.get(i);
        //     arrDistance[i] = euclideanDistance(block.x, block.y, destination.x, destination.y);
        //     if (arrDistance[i] < arrDistance[imin]) {
        //         imin = i;
        //     }
        // }

        /* 
        Y = Kita, O = musuh, A shootable
        X X X X X X
        X O A A A X
        X A A X S S
        X A X A S Y
        X A X X S S

        X X X X X X
        X O B A Y X
        X A A X X X
        X A X A X X
        X A X X X X
        */
        for (Cell attacking : attackingBlocks) {
            if (attacking.x == origin.x && attacking.y == origin.y) {
                return getNextCellToGo(origin, destination);
            }
        }

        for (Cell surrounding: surroundingBlocks) {
            for (Cell attacking: attackingBlocks) {
                int distance = euclideanDistance(surrounding.x, surrounding.y, attacking.x, attacking.y);
                if (gameState.currentRound < 150) {    
                    if (distance < minimumDistance && !isToNearFriend(surrounding)) {
                        minimumDistance = distance;
                        nextX = surrounding.x;
                        nextY = surrounding.y;
                    }
                } else {
                    if (distance < minimumDistance) {
                        minimumDistance = distance;
                        nextX = surrounding.x;
                        nextY = surrounding.y;
                    }
                }
            }
        }

        int finalX = nextX;
        int finalY = nextY;
        
        return surroundingBlocks.stream()
                .filter(block -> block.x == finalX && block.y == finalY)
                .findAny()
                .orElse(getNextCellToGo(origin, destination));
    }

    private List<Cell> getAttackingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        for (int i = x - 4; i <= x + 4; i++) {
            if (i != x && isValidCoordinate(i, y)) {
                cells.add(gameState.map[y][i]);
            }
        }

        for (int i = y - 4; i <= y + 4; i++) {
            if (i != y && isValidCoordinate(x, i)) {
                cells.add(gameState.map[i][x]);
            }
        }

        for (int i = -3; i <= 3; i++) {
            if (i != 0) {
                if (isValidCoordinate(x+i, y+i)) {
                    cells.add(gameState.map[y+i][x+i]);
                } 
                if (isValidCoordinate(x-i, y+i)) {
                    cells.add(gameState.map[y+i][x-i]);
                }
            }
        }
        
        return cells;
    }
   
     /*
    O X X X X
    X X Y a a
    X a X
    X
    X
    */
   
   
   
   
   
   
   
   
   
   
   
   
}
