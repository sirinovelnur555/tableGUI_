package rankTable;

import java.io.*;  
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5001;
    private static final List<Team> teamsBase = new ArrayList<>();
    private static final List<ObjectOutputStream> displayClients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                String clientType = (String) in.readObject();

                if ("INPUT".equals(clientType)) {
                    while (true) {
                        Object obj = in.readObject();

                        if (obj instanceof String) {
                            String msg = (String) obj;
                            if (msg.startsWith("DELETE:")) {
                                String teamName = msg.substring(7);
                                deleteTeam(teamName);
                                broadcast();
                            }
                        } else if (obj instanceof Team) {
                            Team t = (Team) obj;
                            updateTeams(t);
                            broadcast();
                        }
                    }
                } else if ("DISPLAY".equals(clientType)) {
                    synchronized (displayClients) { displayClients.add(out); }
                    out.reset();
                    out.writeObject(new ArrayList<>(teamsBase));
                    out.flush();

                    while (!socket.isClosed()) Thread.sleep(2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static synchronized void updateTeams(Team t) {
        boolean found = false;
        for (Team team : teamsBase) {
            if (team.getName().equalsIgnoreCase(t.getName())) {
                team.setPoints(t.getPoints());
                team.setCollateral(t.getCollateral());
                found = true;
                break;
            }
        }
        if (!found) {
            Team newTeam = new Team(t.getName(), t.getPoints());
            newTeam.setCollateral(t.getCollateral());
            teamsBase.add(newTeam);
        }
    }

    private static synchronized void deleteTeam(String name) {
        teamsBase.removeIf(team -> team.getName().equalsIgnoreCase(name));
    }

    private static synchronized void broadcast() {
        Iterator<ObjectOutputStream> it = displayClients.iterator();
        while (it.hasNext()) {
            ObjectOutputStream out = it.next();
            try {
                out.reset();
                out.writeObject(new ArrayList<>(teamsBase));
                out.flush();
            } catch (IOException e) {
                it.remove();
                try { out.close(); } catch(IOException ignored){}
            }
        }
    }
}
