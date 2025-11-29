package com.projeto;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class SistemaOperacional extends Thread {
    private List<Processo> processos;
    private Graph graph;
    private int intervaloScanSO; 
    private int intervaloVisual = 500; 
    private long ultimaVerificacao = 0;
    private boolean executando = true;

    public SistemaOperacional(List<Processo> processos, int intervaloScanSegundos, Graph graph) {
        this.processos = processos;
        this.intervaloScanSO = intervaloScanSegundos * 1000;
        this.graph = graph;
        this.ultimaVerificacao = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            while (executando) {
                Thread.sleep(intervaloVisual);

                long agora = System.currentTimeMillis();
                boolean momentoDoScan = (agora - ultimaVerificacao) >= intervaloScanSO;

                analisarSistema(momentoDoScan);

                if (momentoDoScan) {
                    ultimaVerificacao = agora;
                }
            }
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void analisarSistema(boolean realizarScanDeadlock) {
        if (!executando) return;

        // Limpa visualização anterior (evita erro de concorrência)
        List<String> arestasParaRemover = new ArrayList<>();
        graph.edges().forEach(e -> arestasParaRemover.add(e.getId()));
        arestasParaRemover.forEach(id -> graph.removeEdge(id));
        
        List<List<Integer>> grafoEspera = new ArrayList<>();
        for(int i=0; i < processos.size(); i++) grafoEspera.add(new ArrayList<>());
        
        Set<Processo> todosEsperando = new HashSet<>();
        Set<Processo> emDeadlock = new HashSet<>();

        // --- PARTE A: CONSTRUÇÃO DO GRAFO E DESENHO BÁSICO ---
        for (int i = 0; i < processos.size(); i++) {
            Processo pSolicitante = processos.get(i);
            String idNoSolicitante = "P_" + pSolicitante.getNomeProc();
            Node nodeSolicitante = graph.getNode(idNoSolicitante);
            
            // Cor padrão: Azul
            nodeSolicitante.setAttribute("ui.style", "fill-color: blue;");

            // Desenha o que TEM (Preto)
            for (Integer idRec : pSolicitante.recursosEmUsoIndices) {
                String idNoRec = "R_" + idRec;
                try {
                    graph.addEdge("tem_" + idNoRec + "_" + idNoSolicitante, idNoRec, idNoSolicitante, true)
                         .setAttribute("ui.style", "fill-color: black; size: 2px;");
                } catch (Exception e) {}
            }

            // Desenha o que QUER (Vermelho Tracejado)
            int idRecursoDesejado = pSolicitante.recursoDesejadoIndex;
            if (idRecursoDesejado != -1) {
                String idNoRec = "R_" + idRecursoDesejado;
                try {
                    graph.addEdge("quer_" + idNoSolicitante + "_" + idNoRec, idNoSolicitante, idNoRec, true)
                         .setAttribute("ui.style", "fill-color: red; stroke-mode: dots;");
                } catch (Exception e) {}

                // Adiciona na lista de espera (Laranja Provisório)
                todosEsperando.add(pSolicitante);
                nodeSolicitante.setAttribute("ui.style", "fill-color: orange;");

                // Monta grafo lógico
                for (int j = 0; j < processos.size(); j++) {
                    if (processos.get(j).recursosEmUsoIndices.contains(idRecursoDesejado)) {
                        grafoEspera.get(i).add(j); // Processo i espera Processo j
                        break;
                    }
                }
            }
        }

        // --- PARTE B: LÓGICA DO SO (SCAN) ---
        if (realizarScanDeadlock) {
            System.out.println("\n--- [SO] SCAN ---");

            // 1. Detecta Deadlock (Método BFS - Mais seguro)
            for (int i = 0; i < processos.size(); i++) {
                // Pergunta simples: "Eu consigo voltar para mim mesmo seguindo as setas?"
                if (fazParteDeCiclo(i, grafoEspera)) {
                    emDeadlock.add(processos.get(i));
                }
            }

            // 2. Classificação: Vítimas vs Normais
            Set<Processo> impedidosForaDoCiclo = new HashSet<>(todosEsperando);
            impedidosForaDoCiclo.removeAll(emDeadlock);
            
            Set<Processo> vitimasDoDeadlock = new HashSet<>(); 

            // Verifica se quem sobrou está bloqueado por alguém do Deadlock
            for (Processo p : impedidosForaDoCiclo) {
                int indexP = processos.indexOf(p);
                // Verifica se o caminho desse processo bate num Deadlock
                if (estaBloqueadoPorDeadlock(indexP, grafoEspera, emDeadlock)) {
                    vitimasDoDeadlock.add(p);
                }
            }
            
            // Remove as vítimas da lista de normais
            impedidosForaDoCiclo.removeAll(vitimasDoDeadlock);

            // 3. Relatório e Pintura
            if (!emDeadlock.isEmpty()) {
                System.out.println(">>> PERIGO: DEADLOCK DETECTADO! <<<");
                
                // VERMELHO: Culpados
                for (Processo p : emDeadlock) {
                    System.out.println(" - " + p.getNomeProc() + " [CICLO DEADLOCK]");
                    graph.getNode("P_" + p.getNomeProc()).setAttribute("ui.style", "fill-color: red; size: 40px; stroke-mode: plain; stroke-color: black; stroke-width: 3px;");
                }

                // ROXO: Vítimas
                for (Processo p : vitimasDoDeadlock) {
                    System.out.println(" - " + p.getNomeProc() + " [IMPEDIDO PELO DEADLOCK]");
                    graph.getNode("P_" + p.getNomeProc()).setAttribute("ui.style", "fill-color: magenta; size: 35px;");
                }
                
                // LARANJA: Normais (Reafirma a cor)
                for (Processo p : impedidosForaDoCiclo) {
                    graph.getNode("P_" + p.getNomeProc()).setAttribute("ui.style", "fill-color: orange;");
                }

                System.out.println(">>> Parando todos os processos... <<<");
                for(Processo p : processos) {
                    p.parar();
                }
                // Para a simulação
                executando = false;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "DEADLOCK DETECTADO!\nVermelho: Ciclo Fatal\nRoxo: Travado pelo Ciclo");
                });

            } else {
                if (!impedidosForaDoCiclo.isEmpty()) {
                    System.out.println("Fila Normal: " + impedidosForaDoCiclo.size() + " processos.");
                } else {
                    System.out.println("Sistema Saudável.");
                }
            }
            System.out.println("-------------------------------------------");
        }
    }

    // --- MÉTODO(FORÇA BRUTA / BFS) ---

    // Verifica se, começando do nó 'startNode', conseguimos voltar para 'startNode'
    private boolean fazParteDeCiclo(int startNode, List<List<Integer>> grafo) {
        Queue<Integer> fila = new LinkedList<>();
        fila.add(startNode);
        
        boolean[] visitados = new boolean[processos.size()];
        // Nota: Não marcamos o startNode como visitado logo de cara para podermos encontrá-lo de novo

        while(!fila.isEmpty()) {
            int atual = fila.poll();

            for(int vizinho : grafo.get(atual)) {
                if(vizinho == startNode) {
                    return true; // Achamos o caminho de volta! CICLO CONFIRMADO.
                }
                if(!visitados[vizinho]) {
                    visitados[vizinho] = true;
                    fila.add(vizinho);
                }
            }
        }
        return false;
    }

    // Verifica se o caminho do processo 'startNode' encosta em alguém que está em Deadlock
    private boolean estaBloqueadoPorDeadlock(int startNode, List<List<Integer>> grafo, Set<Processo> emDeadlock) {
        Queue<Integer> fila = new LinkedList<>();
        fila.add(startNode);
        boolean[] visitados = new boolean[processos.size()];
        visitados[startNode] = true;

        while(!fila.isEmpty()) {
            int atual = fila.poll();
            
            // Verifica vizinhos
            for(int vizinho : grafo.get(atual)) {
                // Se o vizinho faz parte do conjunto de Deadlock, então sou uma vítima
                if (emDeadlock.contains(processos.get(vizinho))) {
                    return true;
                }
                
                if(!visitados[vizinho]) {
                    visitados[vizinho] = true;
                    fila.add(vizinho);
                }
            }
        }
        return false;
    }
}