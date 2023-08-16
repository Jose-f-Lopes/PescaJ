import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static java.awt.SystemColor.desktop;

public class GUI {

    String dir="Please Select Directory";
    FileParser parser=new FileParser();
    Randomizer rand=new Randomizer();
    File fileToOpen;

    private void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Random File Wizard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500,150);
        frame.setResizable(false);

        Panel topSection=new Panel();
        Panel dirSection= new Panel();
        Panel optionsSection=new Panel();
        Panel thumbnailSection= new Panel();
        Panel goodSection= new Panel();



        dirSection.setLayout(new GridLayout(2,1,5,5));
        optionsSection.setLayout(new GridLayout(1,2,5,5));
        topSection.setLayout(new GridLayout(1,2,5,5));
        //goodSection.setLayout(new GridLayout(1,3,5,5));
        goodSection.setLayout(new BorderLayout());
        frame.setLayout(new BorderLayout());


        JLabel dirLabel = new JLabel("Hello World");
        dirLabel.setText(dir);
        dirLabel.setHorizontalAlignment(JTextField.CENTER);

        JRadioButton deepSearch=new JRadioButton("Deep Search");
        deepSearch.setSelected(true);
        deepSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parser.setSearchInDepth(deepSearch.isSelected());
                rand.addToRandomize(parser.getFiles());
            }
        });

        ImageIcon thumbnail = new ImageIcon();
        JLabel image=new JLabel(thumbnail);


        JTextField link=new JTextField("Waiting...");
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setForeground(Color.BLUE.darker());
        link.setEditable(false);
        link.setHorizontalAlignment(JTextField.CENTER);
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().open(fileToOpen);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,ex.getMessage(),"ERROR",JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        JButton goBack=new JButton("<<<");
        goBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    File back=rand.goBackOne();
                    fileToOpen=back;
                    link.setText(back.getName());
                }catch (Exception ex){
                    JOptionPane.showMessageDialog(frame,ex.getMessage(),"ERROR",JOptionPane.WARNING_MESSAGE);
                }

            }
        });

        JButton go=new JButton("Randomize");
        go.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File winner=rand.getRandom();
                link.setText(winner.getName());
                fileToOpen=winner;
                Icon ico= FileSystemView.getFileSystemView().getSystemIcon(winner);
                image.setIcon(ico);
                image.repaint();
            }
        });

        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JButton fileChooser = new JButton("Choose File");
        fileChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fc.showOpenDialog(fileChooser);
                if (returnVal==JFileChooser.APPROVE_OPTION){
                    dir=fc.getSelectedFile().getAbsolutePath();
                    dirLabel.setText(dir);
                    parser.buildFileList(dir);
                    rand.addToRandomize(parser.getFiles());
                }
            }
        });

        dirSection.add(dirLabel);
        optionsSection.add(deepSearch);
        optionsSection.add(fileChooser);
        thumbnailSection.add(image);
        goodSection.add(goBack,BorderLayout.WEST);
        goodSection.add(link,BorderLayout.CENTER);
        goodSection.add(go,BorderLayout.EAST);

        dirSection.add(optionsSection);
        topSection.add(dirSection);
        topSection.add(thumbnailSection);
        frame.add(topSection,BorderLayout.CENTER);
        frame.add(goodSection,BorderLayout.SOUTH);


        //Display the window.

        frame.setVisible(true);

        try{
            parser.buildFileList(dir);
            rand.addToRandomize(parser.getFiles());
        }catch (Exception e){

        }
    }
    static Image iconToImage(Icon icon) {

        if (icon instanceof ImageIcon) {
            return ((ImageIcon)icon).getImage();
        }
        else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }


    public static void main(String[] args) {
        GUI myProgram= new GUI();
        myProgram.createAndShowGUI();

    }
}
