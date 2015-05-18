class Main {
    public static void main(String[] args) {
        new TrayIcon();
        boolean started = false;
        try {
            new Server("localhost", 8080, false);
            started = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            new Server("localhost", 8443, true);
            started = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!started)
            System.exit(1);
    }
}