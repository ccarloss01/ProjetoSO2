package threads;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Geral {
	
	public static Semaphore mutex = new Semaphore(1);
	public static ArrayList<Semaphore> recursos = new ArrayList<>();
	
	public Geral() {
		for(int i = 0; i < 10; i++) {
			recursos.add(new Semaphore(1));
		}		
	}
	
	
}
