package threads;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Processo extends Thread {
	
	private Integer tempo = 0;
	private String nome;
	private Integer solicitacao;
	
	public ArrayList<Integer> recursosEmUso = new ArrayList<>();
	Random rand = new Random();
	
	Queue<Liberacao> recursosParaLiberacao = new LinkedList<>();
	
	public Processo(String nome, Integer solicitacao) {
		this.nome = nome;
		this.solicitacao = solicitacao;
	}
	
	public Integer solicitarRecurso() {
		
		Integer id = rand.nextInt(10);
		
		while(recursosEmUso.contains(id)) {
			id = rand.nextInt(10);
		}
		
		recursosEmUso.add(id);
		return id;
		
	}
	
	public void run() {
		
		while(true) {
			try {
				this.sleep(1000);
				tempo++;
				
				try {
					Geral.mutex.acquire();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				while(recursosParaLiberacao.peek() != null && recursosParaLiberacao.peek().tempo == tempo) {
					Geral.recursos.get(recursosParaLiberacao.peek().recurso).release();
					System.out.println("recurso " + recursosParaLiberacao.peek().recurso + " liberado pelo processo " + this.nome + " em " + tempo);
					recursosParaLiberacao.remove();
				}
				Geral.mutex.release();
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if(tempo % solicitacao == 0) {
				
				try {
					Geral.mutex.acquire();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Integer id = solicitarRecurso();
				System.out.println(this.nome + " " + id + " - " + tempo);
				Geral.mutex.release();
				
				try {
					if(Geral.recursos.get(id).availablePermits() == 0) {
						System.out.println(this.nome + " dormiu por causa do recurso " + id);
					}
					Geral.recursos.get(id).acquire();
					this.recursosParaLiberacao.add(new Liberacao(id, tempo + 10));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
