import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

class TrayIcon implements ActionListener {

    TrayIcon() {
        try {
            BufferedImage trayIconImage;
            final PopupMenu popup = new PopupMenu();
            trayIconImage = ImageIO.read(Main.class.getResource("/icon.png"));
            int trayIconWidth = new java.awt.TrayIcon(trayIconImage).getSize().width;
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH));
            final SystemTray tray = SystemTray.getSystemTray();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(this);
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);
            tray.add(trayIcon);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
    }

    public void actionPerformed(ActionEvent e) {
        System.exit(0);
    }
}
