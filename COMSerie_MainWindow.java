package Base_COM_Serie.gui;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import Utilidades.ConfigUtilities;
import Utilidades.threadpool.DefaultExecutorSupplier;
import gui.Visualizers.AppTest;
import gui.Visualizers.DomoBoardGui;
import gui.Visualizers.Visualizer;
import gui.Visualizers.Msg.VisualizerMessages;
import gui.panel.Console;
import modbus.Const_Modbus;
import modbus.Modbus;
import Base_COM_Serie.ConstantesApp;
import Base_COM_Serie.MB_Registers;
import Base_COM_Serie.Messages;
import Base_COM_Serie.MB_Registers.TSwitchState;
import CommTransport.CommTransport;
import CommTransport.Comm.io.ConnTransportAdaption;
import SerialComm.SerialCommTransport;
import SerialComm.gui.SerialConfig;
import SerialComm.net.SerialAction;
import SerialComm.net.SerialConnection;

public class COMSerie_MainWindow {
	private 	boolean 			doExitOnRequest = true;
	private 	Properties 			configApp = new Properties();
	private 	CommTransport 		sn_Transport;
	private 	JCheckBoxMenuItem 	ConsoleSet;	
	private     Console				serialConsole;
	private 	boolean 			resized = false;
	private 	JMenu 				mnMenuSerie;
	private		JLabel 				statusBar_msgLeft;
	private		JTabbedPane 		mainPanel;
	
	private JFrame window;
	private HashMap<String, JTabbedPane> categoryTable = new HashMap<String, JTabbedPane>();
	
	private ArrayList<Visualizer> visualizers;
		
	public COMSerie_MainWindow() {
		ConfigUtilities.loadConfig(configApp, ConstantesApp.CONFIG_FILE);
		
		// Make sure we have nice window decorations
		JFrame.setDefaultLookAndFeelDecorated(true);

		Rectangle maxSize = GraphicsEnvironment.getLocalGraphicsEnvironment()
								.getMaximumWindowBounds();
		
		window = new JFrame(ConstantesApp.WINDOW_TITLE);
		window.setAlwaysOnTop(true);
		//window.setResizable(false);
		window.setExtendedState(JFrame.MAXIMIZED_BOTH);
		window.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				resized = true;
			}
			@Override
			public void mouseReleased(MouseEvent arg0) {
				if(resized){
					
					Rectangle rectangle = window.getContentPane().getBounds();
		        	configApp.setProperty("windowBounds",rectangle.x+", "+rectangle.y+", "+rectangle.width+", "+rectangle.height);
		    		ConfigUtilities.saveConfig(configApp, ConstantesApp.CONFIG_FILE);
		    		resized = false;
				}
			}
		});
		
		window.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {				
				serialConsoleLocation();	
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
				serialConsoleLocation();
			}			
		});
		
		
		window.setLocationByPlatform(true);
		if (maxSize != null) {
			window.setMaximizedBounds(maxSize);
		}
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}			
		});
		
//		String[] portNames = SerialPortList.getPortNames();
        		
		JMenuBar menuBar = new JMenuBar();
		menuBar.setToolTipText("");
		menuBar.setForeground(Color.WHITE);
		menuBar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		menuBar.setMargin(new Insets(5, 5, 5, 5));
		menuBar.setBackground(Color.ORANGE);
		window.setJMenuBar(menuBar);

		JMenu mnNewMenu = new JMenu(Messages.ARCHIVO);
		menuBar.add(mnNewMenu);

		JMenuItem ExitItem = new JMenuItem(Messages.SALIR);
		ExitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});
		
		JMenuItem mntmPanelInicialpruebas = new JMenuItem("Panel Inicial/Pruebas");
		mntmPanelInicialpruebas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String title = VisualizerMessages.PRUEBAS;
				boolean exisPane = false;
				
				for(int n=0;n<mainPanel.getTabCount();n++)
				{			
					if(title.equals(mainPanel.getTitleAt(n))){
						mainPanel.remove(n);
						categoryTable.remove(title);
						exisPane = true;
					}			
				}
				
				if(!exisPane){
					@SuppressWarnings("serial")
					AppTest appTest = new AppTest(VisualizerMessages.PRUEBAS, sn_Transport){
						//@override
						/*
						@SuppressWarnings("unused")
						public void logVisualizer(String Text){
							log(Text);
						}
						*/
					};
					addVisualizer(appTest);
				}
			}
		});
		mnNewMenu.add(mntmPanelInicialpruebas);
		mnNewMenu.add(ExitItem);
		
		JMenu mnDomoboard = new JMenu("DomoBoard");
		mnDomoboard.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String title = VisualizerMessages.DOMOBOARD;
				boolean exisPane = false;
				
				for(int n=0;n<mainPanel.getTabCount();n++)
				{			
					if(title.equals(mainPanel.getTitleAt(n))){
						mainPanel.remove(n);
						categoryTable.remove(title);
						exisPane = true;
					}			
				}
				
				if(!exisPane){

					String slaveAdd = "1";
					@SuppressWarnings("serial")
					DomoBoardGui domoboardGui = new DomoBoardGui(VisualizerMessages.DOMOBOARD, slaveAdd, sn_Transport){
						/*
						//@override
						@SuppressWarnings("unused")
						public void logVisualizer(String Text){
							log(Text);
						}
						*/
					};
					addVisualizer(domoboardGui);
				}
			}
		});
		menuBar.add(mnDomoboard);
		
		//=======================================================
		//             Menú COMUNICACIONES
		//=======================================================
		JMenu mnNewMenu_1 = new JMenu(Messages.COMUNICACIONES);
		mnNewMenu_1.setForeground(Color.BLACK);
		mnNewMenu_1.setBackground(Color.BLACK);
		menuBar.add(mnNewMenu_1);
		
		setMenuSerie(mnNewMenu_1);
		
		initSerialTransport(mnMenuSerie);
				
		ConsoleSet = new JCheckBoxMenuItem(Messages.CONSOLE);
		mnNewMenu_1.add(ConsoleSet);
		ConsoleSet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent paramActionEvent) {
				AbstractButton aButton = (AbstractButton) paramActionEvent.getSource();
		        boolean selected = aButton.getModel().isSelected();
		        
				//SerialConsole serialConsole = new SerialConsole("TEST");
		        if(selected){
		        	serialConsole = new Console();
		        	serialConsole.addActionListener(new ActionListener() {
		    			public void actionPerformed(ActionEvent e) {
		    				ConsoleMessage(e.getActionCommand());
		    			}
		    		});
		        	serialConsole.setVisible(true);
		        	
		        	serialConsoleLocation();
		        }
		        else{ 
		        	serialConsole.Close();
		        }
		        
		        configApp.setProperty(ConstantesApp.SERIALCONSOLE,String.valueOf(selected));
	    		ConfigUtilities.saveConfig(configApp, ConstantesApp.CONFIG_FILE);
			}
		});
		
		window.getContentPane().setLayout(new BorderLayout(0, 0));
		
		//-----------------------------------------------------
		//-------------  STATUS BAR
		//-----------------------------------------------------
		JPanel statusBar = new JPanel();
		window.getContentPane().add(statusBar, BorderLayout.SOUTH);
				
		statusBar_msgLeft = new JLabel(" " + "Good Day!", JLabel.LEFT);
		statusBar_msgLeft.setForeground(Color.black);
		//statusBar_msgLeft.setToolTipText("Tool Tip Here");
		statusBar.add(statusBar_msgLeft);
				
		JLabel welcomedate = new JLabel();
		welcomedate.setOpaque(true);//to set the color for jlabel
		welcomedate.setBackground(Color.black);
		welcomedate.setForeground(Color.WHITE);
		statusBar.add(welcomedate);
		        
		statusBar.setLayout(new BorderLayout());
		statusBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
		statusBar.setBackground(Color.LIGHT_GRAY);
		statusBar.add(statusBar_msgLeft, BorderLayout.WEST);
		statusBar.add(welcomedate, BorderLayout.EAST);
		
		//display date time to status bar
		Timer timer = new Timer (1000, new ActionListener ()
		{
			public void actionPerformed(ActionEvent e)
			{	
				java.util.Date now = new java.util.Date();
				String ss = DateFormat.getDateTimeInstance().format(now);
				welcomedate.setText(ss);
				welcomedate.setToolTipText("Welcome, Today is " + ss);
			}
		});
				
		timer.start();
				
		statusBar_msgLeft.setText(ConstantesApp.STATUSBAR_MSGLEFT);
		
		//-----------------------------------------------------------------------------------
	
		
		
		mainPanel = new JTabbedPane(JTabbedPane.TOP);
		mainPanel.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		window.getContentPane().add(mainPanel, BorderLayout.CENTER);
		
		//-----------------------------------------------------
		//-----------------------------------------------------
		
		visualizers = new ArrayList<Visualizer>();
	}
	
	
	
	protected void addVisualizer(Visualizer visualizer){
		String category = visualizer.getCategory();
		
		JTabbedPane pane = categoryTable.get(category);
		if (pane == null) {
			pane = new JTabbedPane();
			categoryTable.put(category, pane);
			if(visualizer.isCategory()){
				mainPanel.add(category, pane);
			}
			else
				mainPanel.add(category, visualizer.getPanel());
		}
		
		if(visualizer.isCategory())
			pane.add(visualizer.getTitle(), visualizer.getPanel());
		
		visualizers.add(visualizer);	
	}
	
	protected void setSystemMessage(final String message) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				if (message == null) {
					window.setTitle(ConstantesApp.WINDOW_TITLE);
				} else {
					window.setTitle(ConstantesApp.WINDOW_TITLE + " (" + message + ')');
				}
			}
		});
	}
	
	private void log(String Msg){
		if((serialConsole == null)||(!serialConsole.isVisible())){
			System.out.println(Msg);
		}
		else
			serialConsole.log(Msg);
	}
	
	private void exit() {
		if (doExitOnRequest) {
			//stop();
			System.exit(0);
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}
	
	private void setMenuSerie(JMenu mnMenu){
		mnMenuSerie = new JMenu(Messages.SERIE);
		mnMenuSerie.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				log(Messages.LOOKPORTS);
				lookfor_Ports();
			}
		});
		mnMenu.add(mnMenuSerie);
	
		mnMenu.addSeparator();
	}
	
	private void lookfor_Ports(){
		
		//if(((SerialCommTransport)sn_Transport).SetMenu_SerialPorts(mnMenuSerie)>0){
		if(SetMenu_SerialPorts(mnMenuSerie)>0){			
			mnMenuSerie.addSeparator();
			
			JMenuItem mntmNewMenuItem = new JMenuItem(Messages.OPT_SERIE);
			mnMenuSerie.add(mntmNewMenuItem);
			mntmNewMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent paramActionEvent) {
					SerialConfig frame = new SerialConfig(configApp, ConstantesApp.CONFIG_FILE);
					frame.addWindowListener(new WindowAdapter(){
						public void windowClosed(WindowEvent e)
						  {
							window.setAlwaysOnTop(true);
						  }
			
		            });
					window.setAlwaysOnTop(false);
									
			        frame.setVisible(true); //necessary as of 1.3
					frame.setAlwaysOnTop(true);
			        frame.toFront();
				}
			});
		}
	}
	
	public int SetMenu_SerialPorts(JMenu mnMenu){
		  
		//Obtenemos los puertos Serie disponibles
		
		SerialConnection serialConnection = ((SerialCommTransport)sn_Transport).getSerialConnection();
		
		List<String> ports = serialConnection.ComPortList();
		
		if(ports.size() == 0){
			mnMenu.setEnabled(false);
		}
		else
		{		
			mnMenu.removeAll();
		
			for(String port : ports){
				JRadioButtonMenuItem mntmPrueba = new JRadioButtonMenuItem(port);
				mnMenu.add(mntmPrueba);
				
				//mntmPrueba.addActionListener(connectSerialAction);   
				mntmPrueba.addActionListener(new SerialAction("Connect to serial", serialConnection)); 
			}
		
			
		}
				
		return ports.size();
	}
	
	private void serialConsoleLocation(){
		if(serialConsole != null){
			if(serialConsole.isVisible()){ 
				Dimension windowSize = window.getSize();
				serialConsole.setSize(serialConsole.getWidth(), windowSize.height);
    	
				Point WinLocation = window.getLocation(); 
    	
				serialConsole.setLocation(WinLocation.x + windowSize.width, WinLocation.y);
			}
		}
	}
	
	private void ConsoleMessage(final String message){
		if(message == "Close"){
//			serialServer.serialConsole = null;
			ConsoleSet.setState(false);
		}
	}
	
	//Iniciamos la clase que soporta las comunicaciones
	private void initSerialTransport(JMenu mnMenu){
		
		sn_Transport = new SerialCommTransport(ConstantesApp.SERIALCONNECTION);
		sn_Transport.loadConfig(configApp);
		//sn_Transport.
		sn_Transport.addTransportListener(new ConnTransportAdaption(){
			@Override
			public void logTransport(String message){
				log(message);
			}
			
			@Override 
			public void SystemMessage(String message){
				setSystemMessage(message);
			}
			
			@Override 
			public void CT_Opened(String message){
				statusBar_msgLeft.setText(ConstantesApp.STATUSBAR_MSGLEFT +  message);
			}
			
			/*
			@Override
			public void CTInData(Connection connection, String message){
				if (visualizers != null) {
					for (int i = 0, n = visualizers.size(); i < n; i++) {
						visualizers.get(i).netDataReceived(connection, message);
					}
				}
			}
			*/
		});
		
		lookfor_Ports();				
	}
	
	public void start() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				window.setVisible(true);
				
				String s = configApp.getProperty("windowBounds");
				if(s != null){
					String[] r = s.split(", ");
					window.setBounds(new Rectangle(
			                        Integer.parseInt(r[0]),
			                        Integer.parseInt(r[1]),
			                        Integer.parseInt(r[2]),
			                        Integer.parseInt(r[3])
			                       )
			        );
				} else window.setSize(new Dimension(799, 535)); 
				
				//Centrar ventana.
				//Centrar ventana.
				int scWidth = 0;
				//int scHeigth = 0;
				if(Boolean.valueOf(configApp.getProperty(ConstantesApp.SERIALCONSOLE))){
					ConsoleSet.doClick();
					scWidth = serialConsole.getWidth();
					//scHeigth = serialConsole.getHeight();
				}
				
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		        int X = (screenSize.width - (window.getWidth()+scWidth))/2;
		        int Y = (screenSize.height - window.getHeight())/2;
		        window.setLocation(X, Y);
			}
		});
	}
}
