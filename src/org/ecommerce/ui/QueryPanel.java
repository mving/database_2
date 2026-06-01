package org.ecommerce.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class QueryPanel extends JPanel {
    protected final JLabel status = new JLabel("Sin conectar");
    protected final JTextArea queryArea = new JTextArea(8, 80);
    protected final JTable resultTable = new JTable(new DefaultTableModel());

    public QueryPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);

        queryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    protected JPanel baseConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Conexion"));
        return panel;
    }

    protected JTextField addText(JPanel panel, String label, String value, int row, int col) {
        JTextField field = new JTextField(value);
        addField(panel, label, field, row, col);
        return field;
    }

    protected JPasswordField addPassword(JPanel panel, String label, String value, int row, int col) {
        JPasswordField field = new JPasswordField(value);
        addField(panel, label, field, row, col);
        return field;
    }

    private void addField(JPanel panel, String label, JTextField field, int row, int col) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = row;
        c.gridx = col * 2;
        c.weightx = 0;
        JLabel jLabel = new JLabel(label);
        jLabel.setFont(jLabel.getFont().deriveFont(Font.BOLD));
        panel.add(jLabel, c);

        c.gridx = col * 2 + 1;
        c.weightx = 1;
        field.setPreferredSize(new Dimension(210, 30));
        panel.add(field, c);
    }

    protected JSplitPane queryAndResults(JPanel topPanel) {
        JPanel queryPanel = new JPanel(new BorderLayout(0, 8));
        queryPanel.setBackground(Color.WHITE);
        queryPanel.add(topPanel, BorderLayout.NORTH);
        queryPanel.add(new JScrollPane(queryArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryPanel, new JScrollPane(resultTable));
        split.setResizeWeight(0.44);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    protected JPanel actionBar(JButton connectButton, JButton executeButton, JLabel indicator) {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setBackground(Color.WHITE);
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.add(indicator);
        status.setForeground(new Color(89, 96, 105));
        leftPanel.add(status);
        actions.add(leftPanel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(connectButton);
        buttons.add(executeButton);
        actions.add(buttons, BorderLayout.EAST);
        return actions;
    }

    protected void setRows(List<Map<String, Object>> rows) {
        DefaultTableModel model = new DefaultTableModel();
        if (rows.isEmpty()) {
            model.addColumn("Resultado");
            model.addRow(new Object[]{"Sin resultados"});
            resultTable.setModel(model);
            return;
        }

        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        for (String column : columns) {
            model.addColumn(column);
        }
        for (Map<String, Object> row : rows) {
            Object[] values = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                values[i] = row.get(columns.get(i));
            }
            model.addRow(values);
        }
        resultTable.setModel(model);
    }

    protected void setStatusOk(String text) {
        status.setText(text);
        status.setForeground(new Color(28, 111, 73));
    }

    protected void setStatusError(String text) {
        status.setText(text);
        status.setForeground(new Color(173, 54, 49));
    }

    protected String password(JPasswordField field) {
        return new String(field.getPassword());
    }
}
