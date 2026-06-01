package org.ecommerce.ui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class EcommerceDatabaseUi extends JFrame {
    private final CassandraPanel cassandraPanel = new CassandraPanel();
    private final MongoPanel mongoPanel = new MongoPanel();
    private final Neo4jPanel neo4jPanel = new Neo4jPanel();

    public EcommerceDatabaseUi() {
        setTitle("E-Commerce NoSQL - Consultas TP2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 720));

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        root.setBackground(new Color(245, 247, 250));

        JLabel title = new JLabel("Consultas de datos - Cassandra, MongoDB y Neo4j");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        root.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Reporte TP", new ReportPanel(cassandraPanel, mongoPanel, neo4jPanel));
        tabs.addTab("Cassandra", cassandraPanel);
        tabs.addTab("MongoDB", mongoPanel);
        tabs.addTab("Neo4j", neo4jPanel);
        root.add(tabs, BorderLayout.CENTER);

        setContentPane(root);
        
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int h = (int)(screenSize.height * 0.8);
        setSize(new Dimension(1060, h));
        setLocationRelativeTo(null);
    }
}
