package za.co.entelect.challenge.command;

public class SelectCommand implements Command {

    private final int wormID;
    private final String Comm;

    public SelectCommand(int wormID, String Comm) {
        this.wormID = wormID;
        this.Comm = Comm;
    }

    @Override
    public String render() {
        return String.format("select %d;%s", wormID, Comm);
    }
}

// Command jalan = new MoveCommand(x, y);
// SelectCommand(1, jalan.render());
// Command jalan = new MoveCommand(x, y);
// SelectCommand(1, jalan.render());