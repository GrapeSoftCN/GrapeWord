package Test;

import httpServer.booter;

public class TestWord {
	public static void main(String[] args) {
		booter booter = new booter();
		 try {
		 System.out.println("GrapeWord!");
		 System.setProperty("AppName", "GrapeWord");
		 booter.start(1002);
		} catch (Exception e) {
		}
	}
}
