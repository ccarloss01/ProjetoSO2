package threads;

public class Main {
	public static void main(String[] args) {
		Geral g = new Geral();
		Thread pA = new Processo("A", 3);
		pA.start();
		Thread pB = new Processo("B", 4);
		pB.start();
	}
}
