package com.projeto;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

public class InterfaceConfig extends JFrame {
    
    // Listas temporárias para guardar os processos antes de iniciar
    private ArrayList<Processo> listaProcessos = new ArrayList<>();
    
    private DefaultListModel<String> modelRecursos = new DefaultListModel<>();
    private DefaultListModel<String> modelProcessos = new DefaultListModel<>();

    // Campos de texto
    private JTextField txtIntervaloSO = new JTextField("3", 5);
    private JTextField txtNomeRecurso = new JTextField("R"+"1",10);
    private JTextField txtNomeProc = new JTextField("A", 5);
    private JTextField txtTs = new JTextField("2", 3); // Tempo Solicitação
    private JTextField txtTu = new JTextField("5", 3); // Tempo Uso

    public InterfaceConfig() {
        setTitle("Configuração dos Recursos e Processos");
        setSize(1920,1080);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 1)); // Dividido em 3 partes

        // --- PARTE 1: RECURSOS ---
        JPanel panelRec = new JPanel();
        panelRec.setBorder(BorderFactory.createTitledBorder("1. Adicionar Recursos"));
        panelRec.add(new JLabel("Nome:"));
        panelRec.add(txtNomeRecurso);
        JButton btnAddRec = new JButton("Adicionar");
        panelRec.add(btnAddRec);
        panelRec.add(new JScrollPane(new JList<>(modelRecursos)));
        
        btnAddRec.addActionListener(e -> {
            String nome = txtNomeRecurso.getText();
            if(!nome.isEmpty() && Geral.recursos.size() < 10) {
                // Adiciona na classe Geral
                Geral.recursos.add(new Semaphore(1));
                Geral.nomesRecursos.add(nome);
                
                modelRecursos.addElement(nome);
                txtNomeRecurso.setText("R" + (Geral.nomesRecursos.size() + 1));
            }else {
                JOptionPane.showMessageDialog(this, "Nome inválido ou limite de 10 recursos atingido.");
            }
        });

        // --- PARTE 2: PROCESSOS ---
        JPanel panelProc = new JPanel();
        panelProc.setBorder(BorderFactory.createTitledBorder("2. Adicionar Processos"));
        panelProc.add(new JLabel("Nome:")); panelProc.add(txtNomeProc);
        panelProc.add(new JLabel("Ts (s):")); panelProc.add(txtTs);
        panelProc.add(new JLabel("Tu (s):")); panelProc.add(txtTu);
        JButton btnAddProc = new JButton("Adicionar");
        panelProc.add(btnAddProc);
        panelProc.add(new JScrollPane(new JList<>(modelProcessos)));

        btnAddProc.addActionListener(e -> {
            String nome = txtNomeProc.getText();
            if(!nome.isEmpty() && listaProcessos.size() <= 10) {
                int ts = Integer.parseInt(txtTs.getText());
                int tu = Integer.parseInt(txtTu.getText());
                
                // Cria o processo e guarda na lista temporária
                Processo p = new Processo(nome, ts, tu);
                listaProcessos.add(p);
                
                modelProcessos.addElement(nome + " (Ts:" + ts + ", Tu:" + tu + ")");
                
                // Prepara nome do próximo (A -> B -> C...)
                char proximaLetra = (char)(nome.charAt(0) + 1);
                txtNomeProc.setText(String.valueOf(proximaLetra));
                
            }else{
                JOptionPane.showMessageDialog(this, "Nome inválido ou limite de 10 processos atingido.");
            }
        });
        JPanel panelSO = new JPanel();
        panelSO.setBorder(BorderFactory.createTitledBorder("3. Sistema Operacional"));
        panelSO.add(new JLabel("Scan (s):"));
        panelSO.add(txtIntervaloSO);
        JButton btnStart = new JButton("INICIAR SIMULAÇÃO");
        btnStart.setBackground(Color.GREEN);
        panelSO.add(btnStart);

        btnStart.addActionListener(e -> iniciar());

        add(panelRec);
        add(panelProc);
        add(panelSO);
        setVisible(true);
    }

    private void iniciar() {
        if(Geral.recursos.isEmpty() || listaProcessos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Adicione recursos e processos!");
            return;
        }

        // 1. Configurar Grafo
        System.setProperty("org.graphstream.ui", "swing");
        Graph graph = new SingleGraph("Simulacao");
        String css = "node { fill-color: blue; size: 65px; text-color: white; text-style: bold; } " +
                     "node.recurso { fill-color: red; shape: box; } " +
                     "edge { size: 3px; }";
        graph.setAttribute("ui.stylesheet", css);
        graph.display(false);

        // 2. Adicionar Nós Visuais (Recursos)
        int qtdRecursos = Geral.nomesRecursos.size();
        for(int i = 0; i < qtdRecursos; i++) {
            String id = "R_" + i;
            org.graphstream.graph.Node node = graph.addNode(id);
            node.setAttribute("ui.label", Geral.nomesRecursos.get(i));
            node.setAttribute("ui.class", "recurso");
            node.setAttribute("xyz", i * 2, 0.5, 0); 
        }

        // 3. Adicionar Nós Visuais (Processos) e Iniciar Threads
        int qtdProcessos = listaProcessos.size();
        for(int i = 0; i < qtdProcessos; i++) {
            Processo p = listaProcessos.get(i);
            String id = "P_" + p.getNomeProc();
            org.graphstream.graph.Node node = graph.addNode(id);
            node.setAttribute("ui.label", p.getNomeProc());
            node.setAttribute("ui.class", "processo");
            node.setAttribute("xyz", i * 2, 4.5, 0); 

            p.start(); 
        }

        // 4. Iniciar SO
        int scan = Integer.parseInt(txtIntervaloSO.getText());
        new SistemaOperacional(listaProcessos, scan, graph).start();

        dispose();
    }
}