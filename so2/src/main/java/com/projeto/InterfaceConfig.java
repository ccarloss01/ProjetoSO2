package com.projeto;

import javax.swing.*;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

import javax.swing.border.EmptyBorder;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

public class InterfaceConfig extends JFrame {
    
    // Listas temporárias para guardar os processos antes de iniciar
    private List<Processo> listaProcessos = new CopyOnWriteArrayList<>();
    
    private DefaultListModel<String> modelRecursos = new DefaultListModel<>();
    private DefaultListModel<String> modelProcessos = new DefaultListModel<>();

    // Campos de texto
    private Graph graph;
    private JTextField txtIntervaloSO = new JTextField("3", 5);
    private JTextField txtNomeRecurso = new JTextField("R"+"1",10);
    private JTextField txtNomeProc = new JTextField("A", 5);
    private JTextField txtTs = new JTextField("2", 3); // Tempo Solicitação
    private JTextField txtTu = new JTextField("5", 3); // Tempo Uso

    public InterfaceConfig() {
        setTitle("Configuração dos Recursos e Processos");
        setSize(720,480);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(2, 1)); // Dividido em 3 partes
        setLocationRelativeTo(null);

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

        
        JPanel panelSO = new JPanel();
        panelSO.setBorder(BorderFactory.createTitledBorder("2. Sistema Operacional"));
        panelSO.add(new JLabel("Scan (s):"));
        panelSO.add(txtIntervaloSO);
        JButton btnStart = new JButton("INICIAR SIMULAÇÃO");
        btnStart.setBackground(Color.GREEN);
        panelSO.add(btnStart);
        

        btnStart.addActionListener(e -> iniciar());

        add(panelRec);
        add(panelSO);
        setVisible(true);
    }

    private void iniciar() {
        if(Geral.recursos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Adicione recursos!");
            return;
        }

        // Limpa a tela inicial
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        setSize(1080,720);
        setLocationRelativeTo(null);
        
        // --- PAINEL ESQUERDO (CRIAR PROCESSOS DINAMICAMENTE) ---
        JPanel panelProc = new JPanel();
        panelProc.setLayout(new BoxLayout(panelProc, BoxLayout.Y_AXIS)); // Layout vertical melhor
        panelProc.setBorder(BorderFactory.createTitledBorder("Sistema de Processos"));
        panelProc.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelProc.setPreferredSize(new Dimension(250, 0));

        Dimension tamMax = new Dimension(Integer.MAX_VALUE, 25);
        
        txtNomeProc.setMaximumSize(tamMax);
        txtTs.setMaximumSize(tamMax);
        txtTu.setMaximumSize(tamMax);
        
        panelProc.add(new JLabel("Nome:"));
        panelProc.add(txtNomeProc);
        panelProc.add(Box.createVerticalStrut(5));
        
        panelProc.add(new JLabel("Ts (Solicitação):"));
        panelProc.add(txtTs);
        panelProc.add(Box.createVerticalStrut(5));
        
        panelProc.add(new JLabel("Tu (Utilização):"));
        panelProc.add(txtTu);
        panelProc.add(Box.createVerticalStrut(15));
        
        JButton btnAddDinamico = new JButton("Criar Agora");
        panelProc.add(Box.createVerticalStrut(10));
        panelProc.add(btnAddDinamico);
        
        // Log visual dos processos criados
        DefaultListModel<String> modelDinamico = new DefaultListModel<>();
        // Copia os que já existem para o modelo visual novo
        for(Processo p : listaProcessos) {
            modelDinamico.addElement(p.getNomeProc());
        }
        panelProc.add(new JScrollPane(new JList<>(modelDinamico)));

        // --- AÇÃO DO BOTÃO "CRIAR AGORA" ---
        btnAddDinamico.addActionListener(e -> {
            String nome = txtNomeProc.getText();
            if(nome.isEmpty() || listaProcessos.size() >= 10){
                JOptionPane.showMessageDialog(this, "Nome inválido ou limite de 10 processos atingido.");
            }
            else{
                try {
                    int ts = Integer.parseInt(txtTs.getText());
                    int tu = Integer.parseInt(txtTu.getText());
                    
                    // 1. Cria e guarda
                    Processo p = new Processo(nome, ts, tu);
                    listaProcessos.add(p);
                    modelDinamico.addElement(nome + " (Ts:" + ts + ", Tu:" + tu + ")");
                    
                    // 2. Prepara próximo nome
                    char proximaLetra = (char)(nome.charAt(0) + 1);
                    txtNomeProc.setText(String.valueOf(proximaLetra));
                    
                    // 3. Adiciona no Grafo visualmente
                    String id = "P_" + p.getNomeProc();
                    org.graphstream.graph.Node node = graph.addNode(id);
                    node.setAttribute("ui.label", p.getNomeProc());
                    node.setAttribute("ui.class", "processo");
                    node.setAttribute("xyz", (listaProcessos.size()-1) * 2, 4.5, 0); 
                    
                    // 4. Inicia a thread imediatamente
                    p.start(); 

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Números inválidos.");
                }
            }
        });

        // --- CONFIGURAÇÃO DO GRAFO (DIREITA) ---
        System.setProperty("org.graphstream.ui", "swing");
        graph = new SingleGraph("Simulacao");
        String css = "node { fill-color: blue; size: 40px; text-color: black; text-style: bold; text-alignment: above; } " +
                     "node.recurso { fill-color: red; shape: box; } " +
                     "edge { size: 2px; }";
        graph.setAttribute("ui.stylesheet", css);
        
        // Cria o Painel do Grafo
        org.graphstream.ui.view.Viewer viewer = new org.graphstream.ui.swing_viewer.SwingViewer(graph, org.graphstream.ui.view.Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        org.graphstream.ui.view.View view = viewer.addDefaultView(false); 
        JPanel painelDoGrafo = (JPanel) view;

        // Desenha Recursos Iniciais
        for(int i = 0; i < Geral.nomesRecursos.size(); i++) {
            String id = "R_" + i;
            org.graphstream.graph.Node node = graph.addNode(id);
            node.setAttribute("ui.label", Geral.nomesRecursos.get(i));
            node.setAttribute("ui.class", "recurso");
            node.setAttribute("xyz", i * 2, 1, 0); 
        }

        // Desenha e Inicia Processos Iniciais (que foram criados na tela anterior)
        for(int i = 0; i < listaProcessos.size(); i++) {
            Processo p = listaProcessos.get(i);
            String id = "P_" + p.getNomeProc();
            org.graphstream.graph.Node node = graph.addNode(id);
            node.setAttribute("ui.label", p.getNomeProc());
            node.setAttribute("ui.class", "processo");
            node.setAttribute("xyz", i * 2, 5, 0); 
            p.start(); 
        }

        // Inicia o SO
        int scan = Integer.parseInt(txtIntervaloSO.getText());
        new SistemaOperacional(listaProcessos, scan, graph).start();

        // Monta a janela dividida
        add(panelProc, BorderLayout.WEST);
        add(painelDoGrafo, BorderLayout.CENTER);
        
        // Atualiza a visualização (SEM dispose)
        revalidate();
        repaint();
    }
}