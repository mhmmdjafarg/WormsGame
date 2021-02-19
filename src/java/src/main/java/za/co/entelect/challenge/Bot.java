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

        /* opsional kalau musuh tinggal satu dan kita punya jumlah banyak, gunakan select */
        if(EnemyJustOne()){
            selected = true;
            index = currentWorm.id-1;
        }

        Worm enemyWorm;
        if (selected && gameState.myPlayer.remainingWormSelections > 0) {
            /* Gunakan command select */
            /* Jika current worm merupakan commando, cek apakah worm lain dapat menembakkan skill kepada musuh atau tidak*/
            int idxEnemyAgent = isAnyEnemyThrowable("Agent");
            if(meWorm[index].profession.equals("Commando")){
                if(idxEnemyAgent != -1){
                    if (IsWormCanThrow(gameState.myPlayer.worms[1])) {
                        Command Banana = new BananaCommand(opponent.worms[idxEnemyAgent].position.x, opponent.worms[idxEnemyAgent].position.y);
                        return new SelectCommand(2, Banana.render());
                    } 
                }
                idxEnemyAgent = isAnyEnemyThrowable("Technologist");
                if(idxEnemyAgent != -1){
                    /* Periksa apakah masih ada peluru dan tidak sedang terkena freeze */
                    enemyWorm = getFirstWormInRange(meWorm[index]);
                    if (IsWormCanThrow(gameState.myPlayer.worms[2]) && (enemyWorm != null)){
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
            // idxEnemyAgent = isAnyEnemyThrowable("Technologist");
            enemyWorm = anyWormCanShoot();
            if(meWorm[index].snowBalls != null && meWorm[index].snowBalls.count > 0 && (enemyWorm != null) && !isWormFrozen(enemyWorm)){
                Command snowball = new SnowCommand(enemyWorm.position.x, enemyWorm.position.y);
                return new SelectCommand(index+1, snowball.render());
            }

            /* Tembakan basic jika ada worm yang dapat ditembak, maka langsung tembak*/
            enemyWorm = getFirstWormInRange(meWorm[index]);
            if (enemyWorm != null) {
                Direction direction = resolveDirection(meWorm[index].position, enemyWorm.position);
                Command Shoot = new ShootCommand(direction);
                return new SelectCommand(index+1, Shoot.render());
            }

            enemyWorm = getWormToHunt(index+1);
            return digAndMoveTo(currentWorm.position, enemyWorm.position); 

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
                    enemyWorm = getFirstWormInRange(currentWorm);
                    if (IsWormCanThrow(gameState.myPlayer.worms[2]) && (enemyWorm != null)){
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
            // idxEnemyAgent = isAnyEnemyThrowable("Technologist");
            enemyWorm = anyWormCanShoot();
            if(currentWorm.snowBalls != null && currentWorm.snowBalls.count > 0 && enemyWorm != null && !isWormFrozen(enemyWorm) ){
                return new SnowCommand(enemyWorm.position.x, enemyWorm.position.y);
            }

            /* Jika sudah sendiri dan score kita sudah unggul, maka kabur kaburan*/
            if(WeAreAlone() && gameState.myPlayer.score > opponent.score){
                boolean enemyStronger = false;
                int i = 0;
                while(!enemyStronger && i < 3){
                    if(currentWorm.health < opponent.worms[i].health){
                        enemyStronger = true;
                    }
                    i++;
                }
                if(enemyStronger){
                    Position Dest = new Position();
                    Dest = randomPosition();
                    return digAndMoveTo(currentWorm.position, Dest);
                }
            }

            /* Tembakan basic */
            enemyWorm = getFirstWormInRange(currentWorm);
            if (enemyWorm != null) {
                Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
                return new ShootCommand(direction);
            }

            /* Tidak ada musuh dalam jarak tembak, lakukan dig atau move dengan arah mendekati musuh*/
            enemyWorm = getWormToHunt(currentWorm.id);
            return digAndMoveTo(currentWorm.position, enemyWorm.position);            
        }
    }


    /* Permekanikan gerak dan map */

    /* Mengambil cell yang berada di sekitar */
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

    /* Mengambil posisi random dari cell sekitar */
    private Position randomPosition() {
        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        Cell randomCell;
        boolean permitted = true;
        do {
            randomCell = surroundingBlocks.get( (int) Math.random()*(surroundingBlocks.size()-1));
            for (int i = 0; i < 3; ++i) {
                if (opponent.worms[i].position.x == randomCell.x && opponent.worms[i].position.y == randomCell.y && opponent.worms[i].health > 0){
                    permitted = false;
                }
            }   
        }while (!permitted);

        Position Dest = new Position();
        Dest.x = randomCell.x;
        Dest.y = randomCell.y;
        return Dest;
    }

    /* Cari cell selanjutnya yang mengarah ke tujuan (destination) */
    private Cell getNextCellToGo(Position origin, Position destination) {
        /* list semua cell di sekitar, 
        setiap cellnya akan dihitung jarak euclidean ke dest,*/
        List<Cell> surroundingBlocks = getSurroundingCells(origin.x, origin.y);
        int size = surroundingBlocks.size();
        int[] arrDistance = new int[size];
        boolean near;
        boolean dirt = true;

        /* pilih cell yang jaraknya paling minimum */
        int imin = 0;
        for (int i = 0; i < size; i++) {
            Cell block = surroundingBlocks.get(i);
            //prior
            if (isCellPowerUp(block)) { //powerup pasti di block air
                return block;
            }
            arrDistance[i] = euclideanDistance(block.x, block.y, destination.x, destination.y);
            if (gameState.currentRound < 150) {
                near = isToNearFriend(block);
            } else {
                near = false;
            }
            if (arrDistance[i] < arrDistance[imin] && !near && block.type != CellType.LAVA) {
                imin = i;
                dirt = (block.type == CellType.DIRT);
            } else if (arrDistance[i] == arrDistance[imin] && !near && block.type != CellType.LAVA) {
                if (dirt && block.type != CellType.DIRT) {
                    imin = i;
                    dirt = false;
                }
            }
        }
        return surroundingBlocks.get(imin);
    }

    private List<Cell> getAttackingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 4; i <= x + 4; i++) {
            if ((i != x && i != x+1 && i != x-1) && isValidCoordinate(i, y)) {
                cells.add(gameState.map[y][i]);
            }
        }

        for (int i = y - 4; i <= y + 4; i++) {
            if ((i != y && i != y+1 && i != y-1) && isValidCoordinate(x, i)) {
                cells.add(gameState.map[i][x]);
            }
        }

        for (int i = -3; i <= 3; i++) {
            if (i != 0 && i != 1 && i != -1) {
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

    private Cell getNextCellToAttack(Position origin, Position destination) {
        /* list semua cell di sekitar, 
        setiap cellnya akan dihitung jarak euclidean ke dest,*/
        List<Cell> surroundingBlocks = getSurroundingCells(origin.x, origin.y);
        List<Cell> attackingBlocks = getAttackingCells(destination.x, destination.y);

        int minimumDistance = euclideanDistance(origin.x, origin.y, destination.x, destination.y);
        int minimumDistance2 = minimumDistance;
        boolean dirt = true;
        int nextX = origin.x;
        int nextY = origin.y;

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
                        minimumDistance2 = euclideanDistance(attacking.x, attacking.y, destination.x, destination.y);
                        dirt = (surrounding.type == CellType.DIRT);
                        nextX = surrounding.x;
                        nextY = surrounding.y;
                    } else if (distance == minimumDistance && !isToNearFriend(surrounding)) {
                        if (dirt && surrounding.type != CellType.DIRT) {
                            minimumDistance2 = euclideanDistance(attacking.x, attacking.y, destination.x, destination.y);
                            dirt = false;
                            nextX = surrounding.x;
                            nextY = surrounding.y;
                        } else {
                            int distance2 = euclideanDistance(attacking.x, attacking.y, destination.x, destination.y);
                            if (distance2 < minimumDistance2) {
                                minimumDistance2 = euclideanDistance(attacking.x, attacking.y, destination.x, destination.y);
                                dirt = (surrounding.type == CellType.DIRT);
                                nextX = surrounding.x;
                                nextY = surrounding.y;
                            }
                        }
                    }
                } else {
                    if (distance < minimumDistance) {
                        minimumDistance = distance;
                        minimumDistance2 = euclideanDistance(attacking.x, attacking.y, destination.x, destination.y);
                        dirt = (surrounding.type == CellType.DIRT);
                        nextX = surrounding.x;
                        nextY = surrounding.y;
                    } else if (distance == minimumDistance) {
                        if (dirt && surrounding.type != CellType.DIRT) {
                            minimumDistance2 = euclideanDistance(attacking.x, attacking.y, destination.x, destination.y);
                            dirt = false;
                            nextX = surrounding.x;
                            nextY = surrounding.y;
                        } else {
                            int distance2 = euclideanDistance(attacking.x, attacking.y, destination.x, destination.y);
                            if (distance2 < minimumDistance2) {
                                minimumDistance2 = distance2;
                                dirt = (surrounding.type == CellType.DIRT);
                                nextX = surrounding.x;
                                nextY = surrounding.y;
                            }
                        }
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

    /* Command untuk berjalan atau dig ke arah block mendekati destination */
    private Command digAndMoveTo(Position Origin, Position destination){
        // Cell block = getNextCellToGo(Origin, destination);
        Cell block;
        if (euclideanDistance(Origin.x, Origin.y, destination.x, destination.y) <= 7 ) {
            block = getNextCellToAttack(Origin, destination);
        } else {
            block = getNextCellToGo(Origin, destination);
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


    /* Pertembak-tembakan */

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

    /* Mengembalikan worm musuh yang dapat ditembak oleh worm manapun*/
    private Worm anyWormCanShoot() {
        // dipake ketika mau nembak snowball
        // cek worm kita ada yang bisa nembak si target gak
        for (int i = 0; i < 3; i++) {
            Worm canBeShot = getFirstWormInRange(gameState.myPlayer.worms[i]);
            // cek juga si technologist nya bisa ngelempar snowball ke target gk?
            if (canBeShot != null && isEnemyInThrowRange(canBeShot.position , gameState.myPlayer.worms[2].position, 5)) {
                return canBeShot;
            }
        }
        return null; 
    }

    /* Mengumpulkan cell yang dapat ditembak */
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


    /* Menentukan arah yang sesuai untuk tembak */
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



    /* Per-ultimate-an */

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

    private boolean isEnemyInThrowRange(Position enemyPosition, Position launcher, int range) {
        // memeriksa apakah enemyPosition berada pada jarak lemparan
        return euclideanDistance(launcher.x, launcher.y, enemyPosition.x, enemyPosition.y) <= range;
    }

    /* Memeriksa apakah worm masih punya amunisi, hidup dan tidak terkena freeze */
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
                if (radiusBetweenFriend < 2){
                    return true;
                }
            }
        }
        return false;
    }


    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private boolean isCellPowerUp(Cell my_cell) {
        return (my_cell.powerUp != null);
    }

    private boolean isWormFrozen(Worm worm){
        return (worm.roundsUntilUnfrozen > 0);
    }

    private boolean EnemyJustOne(){
        int count = 0;
        for(int i = 0; i < 3; i++){
            if(opponent.worms[i].health > 0){
                count++;
            }
        }
        return (count == 1);
    }

    private boolean WeAreAlone(){
        int count = 0;
        for(int i =0; i < 3; i++){
            if(gameState.myPlayer.worms[i].health>0){
                count++;
            }
        }
        return (count == 1);
    }
}
