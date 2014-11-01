package nio.finalchat;

import javax.swing.JFrame;

public class dialogInit extends JFrame {

	private static final long serialVersionUID = 1L;

	public dialogInit(){
		this.setTitle("Ma première fenêtre Java");
		this.setSize(400, 500);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);             
		this.setVisible(true);
	}
}