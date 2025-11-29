package com.projeto;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Processo extends Thread {
    
    private Integer tempo = 0;
    private String nome;
    private Integer solicitacao; // Tempo entre solicitações (Delta Ts)
    private Integer tempoUso;    // Tempo de uso (Delta Tu)
    
    public volatile int recursoDesejadoIndex = -1; // -1 = não quer nada agora
    private volatile boolean ativo = true;
    public ArrayList<Integer> recursosEmUsoIndices = new ArrayList<>();


    Random rand = new Random();
    Queue<Liberacao> recursosParaLiberacao = new LinkedList<>();
    
    public Processo(String nome, Integer solicitacao, Integer tempoUso) {
        this.nome = nome;
        this.solicitacao = solicitacao;
        this.tempoUso = tempoUso;
    }
    
    public Integer solicitarRecurso() {
        if (Geral.recursos.isEmpty()) return -1;

        // Escolhe um ID aleatório baseado na quantidade de recursos criados
        Integer id = rand.nextInt(Geral.recursos.size());
        while(recursosEmUsoIndices.contains(id)) {
            id = rand.nextInt(Geral.recursos.size());
        }
        return id;
    }
    public void parar() {
        this.ativo = false;
        this.interrupt(); // Acorda a thread se estiver dormindo no sleep()
    }
    
    public void run() {
        while(ativo) {
            try {
                this.sleep(1000);
                tempo++;
                
                // --- FASE DE LIBERAÇÃO ---
                try {
                    Geral.mutex.acquire();
                    // Verifica se chegou a hora de liberar algum recurso
                    while(recursosParaLiberacao.peek() != null && recursosParaLiberacao.peek().tempo <= tempo) {
                        Liberacao lib = recursosParaLiberacao.poll(); // Tira da fila
                        
                        // Libera o Semáforo
                        Geral.recursos.get(lib.recurso).release();
                        
                        // Remove da lista visual e do console
                        // O cast (Object) é para remover pelo Valor e não pelo Index
                        recursosEmUsoIndices.remove((Object)lib.recurso); 
                        
                        System.out.println("[" + this.nome + "] LIBEROU " + Geral.nomesRecursos.get(lib.recurso) + " no tempo " + tempo);
                    }
                } catch (Exception e) { e.printStackTrace(); } 
                finally { Geral.mutex.release(); }
                
                // --- FASE DE SOLICITAÇÃO ---
                if(tempo % solicitacao == 0) {
                    Integer id = solicitarRecurso();
                    
                    if (id != -1 && !recursosEmUsoIndices.contains(id)) {
                        
                        // 1. Avisa o grafo que QUER este recurso (Desenha linha P -> R)
                        this.recursoDesejadoIndex = id;
                        System.out.println("[" + this.nome + "] REQUER " + Geral.nomesRecursos.get(id));
                        
                        // 2. Tenta pegar o semáforo (Bloqueia aqui se estiver ocupado)
                        Geral.recursos.get(id).acquire();
                        
                        // --- CONSEGUIU ---
                        System.out.println("[" + this.nome + "] TEM " + Geral.nomesRecursos.get(id) + " no tempo " + tempo);
                        
                        // 3. Atualiza status visual
                        this.recursoDesejadoIndex = -1; // Não deseja mais, já tem
                        this.recursosEmUsoIndices.add(id);
                        
                        // 4. Agenda liberação futura
                        this.recursosParaLiberacao.add(new Liberacao(id, tempo + tempoUso));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getNomeProc() { return nome; }
}