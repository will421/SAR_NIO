package nio.finalchat;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nio.multicast.MulticastEntryServer;

public class dialogInit extends JFrame implements ActionListener {

	JButton launch = null;
	JButton stop = null;
	JComboBox<String> listNbClients =null;
	Checkbox autoJoin;
	String _adrServer;
	int _portServer;


	public dialogInit(String adrServer, int portServer){

		_adrServer =adrServer;
		_portServer = portServer;
		
		
		launch = new JButton("Launch");
		stop = new JButton("Stop");
		autoJoin = new Checkbox("Auto-JOIN",true);
		
		
		String[] choixList = {"1","2","3","4","8"};
		listNbClients = new JComboBox(choixList);
		JLabel clientsLabel = new JLabel(" Nombre de clients à lancer pour démo :");
		
		JPanel contentPaneSouth = new JPanel(new GridLayout(0,2));
		JPanel contentPane = new JPanel(new BorderLayout());
		JPanel contentPaneCenter= new JPanel(new BorderLayout());
		 

		this.setTitle("Demo - MultiCast ");
		this.setSize(250, 125);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);             
		this.setResizable(false);


		this.setContentPane(contentPane);

		launch.addActionListener(this);
		stop.addActionListener(this);
		contentPane.add(contentPaneSouth,BorderLayout.SOUTH);
		contentPane.add(contentPaneCenter,BorderLayout.CENTER);

		contentPaneCenter.add(listNbClients,BorderLayout.SOUTH);
		//contentPaneCenter.add(clientsLabel,BorderLayout.CENTER);
		contentPaneCenter.add(autoJoin);
		contentPaneSouth.add(launch);
		contentPaneSouth.add(stop);

		this.setVisible(true);

	}



	@Override
	public void actionPerformed(ActionEvent evt) {

		MulticastEntryServer entryServ;
		
		if (evt.getSource()==launch){

			Boolean autoJoinDebug = autoJoin.getState();
			
			int nbrofClients;
			Object o =listNbClients.getSelectedItem();
			nbrofClients = Integer.parseInt(o.toString());
			
			try {
				entryServ = new MulticastEntryServer("localhost", 8888, nbrofClients);
				
				Thread t = new Thread(entryServ);
				t.start();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			
			for(int i =0; i < nbrofClients ;i++){

				Thread t = null;
				try {
					t = new Thread(new ChatRoomFinal(_adrServer,_portServer,autoJoinDebug));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				t.start();

			}
		}

		else if(evt.getSource()==stop) {

			System.exit(-1);

		}




	}


}